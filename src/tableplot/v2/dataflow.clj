(ns tableplot.v2.dataflow
  "Core dataflow model for plotting.
  
  A plot spec is a map with subkeys (substitution keys) that get resolved through inference.
  
  Key ideas:
  - Subkeys are placeholders for values to be inferred/substituted
  - By convention, we use := prefix (e.g., :=data, :=x-scale)
  - Templates declare their subkeys via :sub/keys
  - Specs flow through transformations that add/modify :sub/map
  - A single `infer` pass resolves all subkeys using registered rules"
  (:require [clojure.walk :as walk])
  (:import [clojure.lang IPersistentMap IPersistentVector]))

;;; ============================================================================
;;; Subkey Protocol
;;; ============================================================================

(defn dataset-or-column?
  "Check if x is a tech.v3.dataset or column (without hard dependency)"
  [x]
  (and (some? x)
       (let [class-name (.getName (class x))]
         (or (.contains class-name "dataset.Dataset")
             (.contains class-name "dataset.impl")
             (.contains class-name "Column")))))

(defn subkey-by-convention?
  "Check if a keyword looks like a subkey by convention (starts with =).
  
  This is just a helper convention - the real source of truth is :sub/keys."
  [k]
  (and (keyword? k)
       (clojure.string/starts-with? (name k) "=")))

(defn get-subkeys
  "Get the set of subkeys for a template/spec.
  
  Checks :sub/keys first, falls back to convention."
  [template-or-spec]
  (or (:sub/keys template-or-spec)
      ;; Fall back to inferring from convention
      (let [found (atom #{})]
        (letfn [(walk-for-subkeys [x]
                  (cond
                    ;; Found a subkey
                    (subkey-by-convention? x)
                    (swap! found conj x)

                    ;; Don't descend into datasets/columns
                    (dataset-or-column? x)
                    nil

                    ;; Descend into maps
                    (map? x)
                    (doseq [[k v] x]
                      (walk-for-subkeys k)
                      (walk-for-subkeys v))

                    ;; Descend into vectors/seqs
                    (or (vector? x) (seq? x))
                    (doseq [item x]
                      (walk-for-subkeys item))))]
          (walk-for-subkeys template-or-spec))
        @found)))

(defn subkey?
  "Check if a keyword is a subkey in the given spec/template"
  [spec k]
  (contains? (get-subkeys spec) k))

(defn find-subkeys
  "Find all subkeys in a nested data structure.
  
  Stops descending into datasets and other non-plain-data objects.
  Excludes the special :sub/keys and :sub/map keys."
  [spec]
  (let [keys (atom #{})]
    (letfn [(walk-for-keys [x]
              (cond
                ;; Found a subkey (but not meta keys)
                (and (subkey? spec x)
                     (not (#{:sub/keys :sub/map} x)))
                (swap! keys conj x)

                ;; Don't descend into datasets
                (dataset-or-column? x)
                nil

                ;; Descend into maps, but skip the meta keys
                (map? x)
                (doseq [[k v] x]
                  (when-not (#{:sub/keys :sub/map} k)
                    (walk-for-keys k)
                    (walk-for-keys v)))

                ;; Descend into vectors/seqs
                (or (vector? x) (seq? x))
                (doseq [item x]
                  (walk-for-keys item))))]
      (walk-for-keys spec))
    @keys))

;;; ============================================================================
;;; Spec Structure
;;; ============================================================================

(defn make-spec
  "Create a new plot spec with optional substitutions.
  
  A spec has two parts:
  - The template: the plot structure with subkey placeholders
  - The substitutions: concrete values for subkeys (stored in :sub/map)
  
  We keep them in a single map for easy transformation."
  [template & {:as substitutions}]
  (cond-> template
    (seq substitutions)
    (assoc :sub/map substitutions)))

(defn get-substitution
  "Get the value for a subkey"
  [spec k]
  (get-in spec [:sub/map k]))

(defn add-substitution
  "Add a substitution to the spec"
  [spec k v]
  (assoc-in spec [:sub/map k] v))

(defn add-substitutions
  "Add multiple substitutions to the spec"
  [spec substs]
  (update spec :sub/map merge substs))

;;; ============================================================================
;;; Base Templates
;;; ============================================================================

(def base-plot-template
  "Minimal plot template with all the subkeys that can be inferred"
  {:sub/keys #{:=data :=layers
               :=x-scale :=y-scale :=color-scale :=size-scale
               :=x-guide :=y-guide :=color-guide :=size-guide
               :=x-field :=y-field :=color-field :=size-field
               :=facets :=theme :=title :=labels}
   :data :=data
   :layers :=layers
   :scales {:x :=x-scale
            :y :=y-scale
            :color :=color-scale
            :size :=size-scale}
   :guides {:x :=x-guide
            :y :=y-guide
            :color :=color-guide
            :size :=size-guide}
   :facets :=facets
   :theme :=theme
   :title :=title
   :labels :=labels})

(def base-layer-template
  "Minimal layer template"
  {:sub/keys #{:=mark :=data
               :=x-field :=y-field :=color-field :=size-field
               :=x-scale :=y-scale :=color-scale :=size-scale}
   :mark :=mark
   :data :=data ; can override plot-level data
   :encoding {:x {:field :=x-field :scale :=x-scale}
              :y {:field :=y-field :scale :=y-scale}
              :color {:field :=color-field :scale :=color-scale}
              :size {:field :=size-field :scale :=size-scale}}})

;;; ============================================================================
;;; Inference Rules Registry
;;; ============================================================================

(def ^:dynamic *inference-rules*
  "Registry of inference rules. Each rule is a function: (spec context) -> value
  
  Context includes information about where in the spec tree we are."
  {})

(defn register-inference-rule!
  "Register an inference rule for a subkey"
  [k rule-fn]
  (alter-var-root #'*inference-rules* assoc k rule-fn))

(defn infer-key
  "Infer the value for a single subkey using registered rules"
  [spec k context]
  (if-let [rule (get *inference-rules* k)]
    (rule spec context)
    (throw (ex-info (str "No inference rule for " k)
                    {:key k :context context}))))

;;; ============================================================================
;;; Dependency Tracking
;;; ============================================================================

(defn get-rule-dependencies
  "Get the subkeys that a rule depends on (metadata on the rule fn)"
  [rule-fn]
  (::depends-on (meta rule-fn) #{}))

(defn topological-sort
  "Sort subkeys by dependency order so we infer in the right sequence"
  [keys]
  ;; Simple implementation: rules with no deps first, then others
  ;; TODO: proper topological sort with cycle detection
  (let [rules *inference-rules*
        with-deps (fn [k]
                    (get-rule-dependencies (get rules k)))
        no-deps (filter #(empty? (with-deps %)) keys)
        has-deps (remove #(empty? (with-deps %)) keys)]
    (concat no-deps has-deps)))

;;; ============================================================================
;;; Core Inference Engine
;;; ============================================================================

(defn apply-substitutions
  "Walk the spec and replace subkeys with their values from :sub/map.
  
  Stops descending into datasets and other non-plain-data objects."
  [spec]
  (let [subs (get spec :sub/map {})]
    (letfn [(substitute [x]
              (cond
                ;; Replace subkeys
                (subkey? spec x)
                (get subs x x)

                ;; Don't descend into datasets
                (dataset-or-column? x)
                x

                ;; Descend into maps
                (map? x)
                (into {} (map (fn [[k v]] [(substitute k) (substitute v)]) x))

                ;; Descend into vectors
                (vector? x)
                (mapv substitute x)

                ;; Descend into seqs
                (seq? x)
                (map substitute x)

                ;; Leave other values as-is
                :else x))]
      (-> (substitute spec)
          (dissoc :sub/map :sub/keys)))))

(defn infer-missing-keys
  "Find subkeys that aren't in :sub/map and infer them using rules"
  [spec]
  (let [all-keys (find-subkeys spec)
        existing (set (keys (get spec :sub/map {})))
        missing (remove existing all-keys)
        sorted-missing (topological-sort missing)]
    (reduce
     (fn [s k]
       (let [value (infer-key s k {:key k})]
         (add-substitution s k value)))
     spec
     sorted-missing)))

(defn infer
  "Main inference function: resolves all subkeys in the spec
  
  1. Finds all subkeys that don't have substitutions yet
  2. Infers them using registered rules (in dependency order)
  3. Applies all substitutions to get final spec"
  [spec]
  (-> spec
      infer-missing-keys
      apply-substitutions))

;;; ============================================================================
;;; API Functions for Spec Transformation
;;; ============================================================================

(defn with-data
  "Add data to a spec"
  [spec data]
  (add-substitution spec :=data data))

(defn with-layer
  "Add a layer to a spec. Layer can be a map or a subkey"
  [spec layer]
  (let [layers (or (get-substitution spec :=layers) [])]
    (add-substitution spec :=layers (conj layers layer))))

(defn with-layers
  "Set all layers at once"
  [spec layers]
  (add-substitution spec :=layers (vec layers)))

(defn with-aesthetic
  "Add an aesthetic mapping to the most recent layer"
  [spec aesthetic field]
  (let [layers (get-substitution spec :=layers [])
        last-idx (dec (count layers))
        aesthetic-key (keyword (str "=" (name aesthetic) "-field"))]
    (if (>= last-idx 0)
      (add-substitution spec aesthetic-key field)
      (throw (ex-info "No layer to add aesthetic to" {:aesthetic aesthetic})))))

(defn with-scale
  "Set a scale definition"
  [spec aesthetic scale-def]
  (let [scale-key (keyword (str "=" (name aesthetic) "-scale"))]
    (add-substitution spec scale-key scale-def)))

(defn with-title
  "Set the plot title"
  [spec title]
  (add-substitution spec :=title title))

;;; ============================================================================
;;; Example Usage (commented out, for reference)
;;; ============================================================================

(comment
  ;; Start with base template
  (def spec (make-spec base-plot-template))

  ;; Transform it by adding substitutions
  (-> spec
      (with-data :iris)
      (with-layer {:mark :point})
      (with-aesthetic :x :sepal-width)
      (with-aesthetic :y :sepal-length))

  ;; Infer missing values
  (infer spec)

  ;; Or do it all at once
  (-> (make-spec base-plot-template)
      (with-data :iris)
      (with-layer {:mark :point})
      (with-aesthetic :x :sepal-width)
      (with-aesthetic :y :sepal-length)
      infer))
