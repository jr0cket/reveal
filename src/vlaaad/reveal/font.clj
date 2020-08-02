(ns vlaaad.reveal.font
  (:require [clojure.java.io :as io])
  (:import [com.sun.javafx.tk Toolkit]
           [com.sun.javafx.font PGFont FontResource]
           [com.sun.javafx.geom.transform BaseTransform]))

(set! *warn-on-reflection* true)

(set! *unchecked-math* :warn-on-boxed)

(deftype Font [^javafx.scene.text.Font font ^double line-height ^double ascent char-width-cache])

(defmacro ^:private if-class [class-name then else]
  `(try
     (Class/forName ^String ~class-name)
     ~then
     (catch ClassNotFoundException _#
       ~else)))

(def get-native-font
  (if-class "com.sun.javafx.scene.text.FontHelper"
    (let [meth (-> (Class/forName "com.sun.javafx.scene.text.FontHelper")
                   (.getDeclaredMethod "getNativeFont" (into-array Class [javafx.scene.text.Font])))]
      #(.invoke meth nil (into-array Object [%])))
    (let [meth (-> (Class/forName "javafx.scene.text.Font")
                   (.getDeclaredMethod "impl_getNativeFont" (into-array Class [])))]
      #(.invoke meth % (into-array Object [])))))

(def ^javafx.scene.text.Font font
  (javafx.scene.text.Font/loadFont
    (io/input-stream
      (io/resource "vlaaad/reveal/FantasqueSansMono-Regular.ttf")) 48.0))

(let [metrics (.getFontMetrics (.getFontLoader (Toolkit/getToolkit)) font)]
  (def ^double ^:const line-height (Math/ceil (.getLineHeight metrics)))
  (def ^double ^:const ascent (.getAscent metrics)))

(def ^double ^:const char-width
  (-> font
      ^PGFont get-native-font
      (.getStrike BaseTransform/IDENTITY_TRANSFORM FontResource/AA_GREYSCALE)
      (.getCharAdvance \a)))
