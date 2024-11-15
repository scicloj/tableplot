(ns scicloj.tableplot.v1.xform
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [com.rpl.specter :as sp]
            [tech.v3.dataset.impl.dataset]))

(defn xform
  "and adapted port of Hanami's xform function"
  ([spec xkv]
   (let [xkv (if (not (xkv ::hc/spec)) (assoc xkv ::hc/spec spec) xkv)
         defaults @hc/_defaults
         use-defaults? (get xkv ::hc/use-defaults? (defaults ::hc/use-defaults?))
         xkv (if use-defaults? (merge defaults xkv) xkv)
         template-defaults (if (map? spec) (spec ::ht/defaults) false)
         spec (if template-defaults (dissoc spec ::ht/defaults) spec)
         xkv (if template-defaults
               (merge xkv template-defaults (xkv ::hc/user-kvs))
               xkv)]
     (sp/transform
      sp/ALL
      (fn [v]
        (if (coll? v)
          (let [xv (xform v xkv)
                rmv? (xkv ::hc/rmv-empty?)]
            (if (seq xv) xv (if rmv? hc/RMV xv)))
          (let [subval (get xkv v v)
                subval (if (fn? subval) (subval xkv) subval)
                subkeyfn (@hc/subkeyfns v)
                subval (if subkeyfn (subkeyfn xkv v subval) subval)]
            (cond
              ;; leaf value => termination
              (= v subval) v

              ;; Do not xform the data
              (or (= v hc/data-key)
                  (-> subval 
                      type 
                      (= tech.v3.dataset.impl.dataset.Dataset)))
              subval

              ;; Potential new subkey as subval
              (or (string? subval)
                  (not (coll? subval)))
              (recur subval)

              :else ;substitution val is coll
              (let [xv (xform subval xkv)
                    rmv? (xkv ::hc/rmv-empty?)]
                (if (seq xv) xv (if rmv? hc/RMV xv)))))))
      spec)))

  ([spec k v & kvs]
   (let [user-kv-map (into {k v}
                           (->> kvs (partition-all 2)
                                (mapv (fn [[k v]] [k v]))))
         ;; Need to keep these to override new template defaults
         start-kv-map (assoc user-kv-map ::hc/user-kvs user-kv-map)]
     (xform spec start-kv-map)))

  ([spec] (xform spec {})))

