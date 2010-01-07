(ns leiningen.appengine-setup
  (:use clojure.contrib.pprint)
  (:import (java.io File FileReader PushbackReader
		    FileWriter)))

(defn update-dependencies [x]
  (update-in x [:dependencies] conj
	     '[org.clojars.hiredman/compojure "0.3.1"]
	     '[com.google.appengine/appengine-api-1.0-sdk "1.2.1"]
	     '[com.google.appengine/appengine-tools-sdk "1.2.1"]
	     '[geronimo-spec/geronimo-spec-servlet "2.4-rc4"]))

(defn appengine-setup [project]
  (doseq [f ["war/WEB-INF/classes"
	     "war/WEB-INF/lib"
	     (format "src/%s" (:name project))]]
    (.mkdirs (File. (:root project) f)))
  (binding [*out* (-> project :root (File. (format "src/%s/servlet.clj" (:name project)))
		      FileWriter.)]
    (prn (list 'ns (symbol (format "%s.servlet" (:name project)))
	       '(:gen-class :extends javax.servlet.http.HttpServlet)
	       '(:use compojure.html compojure.http)))
    (prn (list 'defroutes (symbol (:name project))
	       '(GET "/*" (html [:h1 "Hello World"]))))
    (prn (list 'defservice (symbol (:name project)))))
  (let [[_ n v & proj] (with-open [f (-> project :root (File. "project.clj")
				  FileReader. PushbackReader.)]
		  (read f))]
    (with-open [f (-> project :root (File. "project.clj")
		      FileWriter.)]
      (binding [*out* f]
	(pprint 
	 (seq
	  (reduce into ['defproject n v]
		  (update-dependencies
		   (merge-with (comp first list)  (apply hash-map proj)
			       {:compile-path "war/WEB-INF/classes"
				:appengine {:id (:name project)
					    :display-name (:name project)
					    :version 1
					    :devport 8080}})))))))))