(ns ^:figwheel-hooks bulletin.core
  (:require [bulletin.icons :as icons]
            [bulletin.util :as util]
            [cljs.core.async :refer [<!]]
            [goog.dom :as gdom]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:token nil :screen :login}))
(defonce timeline-state (atom {}))
(defonce composer-state (atom {:parent-id nil :text nil}))

(defn update-timeline! []
  (go (let [tl (<! (util/fetch-timeline (:token @app-state)))]
        (reset! timeline-state tl))))

(defn reply-post! [])

(defn fave-post! [id]
  (go (let [res (<! (util/fave-post (:token @app-state) id))]
        (when res
          (prn (str "Post " id " favourited!"))))))

(defn get-app-element []
  (gdom/getElement "app"))

(defn login []
  (let [v (atom nil)]
    (fn []
      [:div#login {:class "bg-white justify-center max-w-md mx-auto my-4 p-4 shadow"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (swap! app-state assoc :token @v)
                            (swap! app-state assoc :screen :timeline)
                            (update-timeline!))}
        [:h2 {:class "mb-4 text-center text-xl"} "Almost there..."]
        [:label {:class "font-semibold inline-block w-2/12"} "Token:"]
        [:input {:class "bg-gray-200 inline-block focus:bg-white border border-gray-400 p-2 w-9/12"
                 :type "text"
                 :value @v
                 :placeholder "Enter app token"
                 :on-change #(reset! v (-> % .-target .-value))}]
        [:button {:class "bg-blue-500 hover:bg-blue-700 block mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Submit"]]])))

(defn username-link [username inner]
  [:a {:href (str "https://micro.blog/" username)} inner])

(defn svg [opts size-w size-h path]
  [:svg (merge {:xmlns "http://www.w3.org/2000/svg"
                :viewBox (str "0 0 " size-w " " size-h)} opts)
   [:path {:d path}]])

(defn post-content [html]
  (util/html->hiccup html :remove-style))

(defn timeline []
  (fn []
    [:div#timeline {:class "justify-center max-w-xl mx-auto my-4"}
     (for [p @timeline-state]
       (let [id (:id p)
             username (get-in p [:author :_microblog :username])]
         ^{:key (:id p)}
         [:div.post {:class "bg-white my-4 p-8 shadow"
                     :data-id id}
          (username-link username [:img {:class "float-left rounded-full h-16 mr-4 w-16"
                                         :src (get-in p [:author :avatar])}])
          [:h3 {:class "font-semibold mt-2 text-black text-lg"} (username-link username (get-in p [:author :name]))]
          [:h4 {:class "font-light text-gray-600" } (username-link username (str "@" username))]
          (into [:div.content {:class "mb-5 mt-6 clear-both"}] (post-content (:content_html p)))
          [:form {:class "inline-block"
                  :on-submit (fn [x]
                               (.preventDefault x)
                               (swap! composer-state assoc :parent-id id :text (str "@" username " "))
                               (swap! app-state assoc :screen :composer))}
           [:button {:class "mr-6"}
            (svg {:class "fill-current inline-block text-gray-500 w-5 h-5 hover:text-black"} 20 20 icons/reply)]]
          [:form {:class "inline-block"
                  :on-submit (fn [x]
                               (.preventDefault x)
                               (fave-post! id))}
           [:button {:class "mr-8"}
            (svg {:class "fill-current inline-block text-gray-500 w-5 h-5 hover:text-black"} 20 20 icons/star)]]
          [:a {:href (str "https://micro.blog/" username "/" id)}
           (svg {:class "fill-current inline-block text-gray-300 w-5 h-5 hover:text-black"} 20 20 icons/chat)]
          [:h4 {:class "float-right text-gray-500"}
           [:a {:href (:url p)} (-> (:date_published p) (util/str->date 9) (util/date->str))]]]))]))

(defn composer []
  (let [t (atom nil)]
    (fn []
      [:div#login {:class "bg-white clearfix max-w-md mx-auto my-4 p-4 shadow text-right"}
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (swap! app-state assoc :screen :timeline))}
        [:input {:type "hidden"
                 :name "parent-id"
                 :value (:parent-id @composer-state)}]
        [:textarea {:class "bg-gray-200 focus:bg-white border border-gray-400 h-56 p-2 w-full"
                    :value (:text @composer-state)
                    :placeholder "What do you want to say?"
                    :on-change #(swap! composer-state assoc :text (-> % .-target .-value))}]
        [:button {:class "bg-gray-500 hover:bg-red-700 float-left mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "button"
                  :on-click #(swap! app-state assoc :screen :timeline)} "Cancel"]
        [:button {:class "bg-blue-500 hover:bg-blue-700 float-right mx-auto mt-4 px-4 py-2 rounded text-white"
                  :type "submit"} "Post"]]])))

(defn app-container []
  (case (:screen @app-state)
    :login [login]
    :timeline [timeline]
    :composer [composer]))

(defn mount [el]
  ; (reagent/render-component [timeline] el)
  (reagent/render [app-container] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

; (reset! timeline-state [{:id 2 :author {:name "Testy McTest"} :content_html "A test post."}])
