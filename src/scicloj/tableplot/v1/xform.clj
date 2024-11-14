(ns scicloj.tableplot.v1.xform
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [com.rpl.specter :as sp]))

(defn xform
  ([spec xkv]
   (let [xkv (if (not (xkv ::spec)) (assoc xkv ::spec spec) xkv)
         defaults @hc/_defaults
         use-defaults? (get xkv ::use-defaults? (defaults ::use-defaults?))
         xkv (if use-defaults? (merge defaults xkv) xkv)
         template-defaults (if (map? spec) (spec ::ht/defaults) false)
         spec (if template-defaults (dissoc spec ::ht/defaults) spec)
         xkv (if template-defaults
               (merge xkv template-defaults (xkv ::user-kvs))
               xkv)]
     (sp/transform
      sp/ALL
      (fn [v]
        (if (coll? v)
          (let [xv (xform v xkv)
                rmv? (xkv ::rmv-empty?)]
            (if (seq xv) xv (if rmv? hc/RMV xv)))
          (let [subval (get xkv v v)
                subval (if (fn? subval) (subval xkv) subval)
                subkeyfn (@hc/subkeyfns v)
                subval (if subkeyfn (subkeyfn xkv v subval) subval)]
           (cond
              ;; leaf value => termination
              (= v subval) v

              ;; Do not xform the data
              (= v hc/data-key) subval

              ;; Potential new subkey as subval
              (or (string? subval)
                  (not (coll? subval)))
              (recur subval)

              :else ;substitution val is coll
              (let [xv (xform subval xkv)
                    rmv? (xkv ::rmv-empty?)]
                (if (seq xv) xv (if rmv? hc/RMV xv)))))))
      spec)))

  ([spec k v & kvs]
   (let [user-kv-map (into {k v}
                           (->> kvs (partition-all 2)
                                (mapv (fn [[k v]] [k v]))))
         ;; Need to keep these to override new template defaults
         start-kv-map (assoc user-kv-map ::user-kvs user-kv-map)]
     (xform spec start-kv-map)))

  ([spec] (xform spec {})))

