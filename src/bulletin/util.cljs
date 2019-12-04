(ns bulletin.util
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.walk]
            [goog.date.DateTime]
            [goog.i18n.DateTimeFormat]
            [hickory.core])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn fetch-timeline
  "Fetch the timeline for a user using the supplied `token`. The timeline is
  returned as a map inside a core.async channel."
  [token]
  (go (let [response (<! (http/get "https://micro.blog/posts/all"
                                   {:with-credentials? false
                                    :headers {"Authorization" token}}))]
        (if (= (:status response) 200)
          (:items (:body response))
          (println "Error!")))))

(defn fave-post
  "Mark a post as a favourite."
  [token id]
  (go (let [response (<! (http/post "https://micro.blog/posts/favorites"
                                    {:with-credentials? false
                                     :headers {"Authorization" token}
                                     :json-params {:id id}}))]
        (if (= (:status response) 200)
          true
          (println "Error!")))))

(defn remove-keys
  "Walk a data structure and remove nodes that match a key in a set."
  [key-set data]
  (clojure.walk/prewalk (fn [node]
                          (if (map? node)
                            (apply dissoc node key-set)
                            node))
                        data))

(defn html->hiccup
  "Transforms HTML into Hiccup."
  ([html]
   (->> html
        (hickory.core/parse-fragment)
        (map hickory.core/as-hiccup)))
  ([html & opts]
   (let [ks (set opts)]
     (cond->> (html->hiccup html)
              (contains? ks :remove-style) (remove-keys [:style])))))

(defn str->date
  "Parse a date, `s`, in ISO 8601 format and return a goog.date.DateTime object.
  An hour offset, `n`, can also be provided that will be used to adjust
  the final object."
  ([s]
   (str->date s 0))
  ([s offset]
   (->> s
        (.parse js/Date)
        (+ (* offset 60 60 1000))
        (.fromTimestamp goog.date.DateTime))))

(defn date->str
  "Formats the date, `date`, using the default 'd MMMM yyyy, h:mm a' pattern. An
  alternative pattern, `patt`, can also be provided."
  ([date]
   (date->str date "d MMMM yyyy, h:mm a"))
  ([date patt]
   (.format (new goog.i18n.DateTimeFormat patt) date)))
