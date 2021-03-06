(ns vlaaad.reveal.prepl
  (:require [clojure.core.server :as server]
            [clojure.main :as m]
            [vlaaad.reveal.stream :as stream]
            [vlaaad.reveal.ui :as ui]
            [vlaaad.reveal.style :as style]
            [clojure.string :as str]))

(defn- prepl-output [x]
  (stream/as x
    (if (:exception x)
      (cond-> (stream/raw-string (-> x :val m/ex-triage m/ex-str) {:fill style/error-color})
              (:form x)
              (as-> err-output
                    (stream/vertical
                      (stream/raw-string (:form x) {:fill style/util-color})
                      err-output)))
      (case (:tag x)
        :ret (stream/vertical
               (stream/raw-string (:form x) {:fill style/util-color})
               (stream/horizontal
                 (stream/raw-string "=>" {:fill style/util-color})
                 stream/separator
                 (stream/stream (:val x))))
        :out (stream/raw-string (str/trim-newline (:val x)) {:fill style/string-color})
        :err (stream/raw-string (str/trim-newline (:val x)) {:fill style/error-color})
        :tap (stream/horizontal
               (stream/raw-string "tap>" {:fill style/util-color})
               stream/separator
               (stream/stream (:val x)))
        (stream/emit x)))))

(defn- wrap-out-fn [ui out-fn]
  (fn [x]
    (ui (prepl-output x))
    (out-fn x)))

(defn prepl [in-reader out-fn & {:keys [stdin]}]
  (let [ui (ui/make)]
    (try
      (server/prepl in-reader (wrap-out-fn ui out-fn) :stdin stdin)
      (finally (ui)))))

(defn remote-prepl [host port in-reader out-fn & {:as prepl-args}]
  (let [ui (ui/make)
        prepl-args (update prepl-args :valf (fn [valf]
                                              (or valf
                                                  #(binding [*default-data-reader-fn* tagged-literal]
                                                     (read-string %)))))]
    (try
      (apply server/remote-prepl host port in-reader (wrap-out-fn ui out-fn) (mapcat identity prepl-args))
      (finally (ui)))))

(defn io-prepl [& {:keys [valf] :or {valf pr-str}}]
  (let [ui (ui/make)
        out *out*
        lock (Object.)]
    (try
      (server/prepl
        *in*
        (fn [x]
          (ui (prepl-output x))
          (binding [*out* out *flush-on-newline* true *print-readably* true]
            (locking lock
              (prn (if (#{:ret :tap} (:tag x))
                     (try
                       (assoc x :val (valf (:val x)))
                       (catch Throwable ex
                         (assoc x :val (assoc (Throwable->map ex) :phase :print-eval-result)
                                  :exception true)))
                     x))))))
      (finally (ui)))
    nil))
