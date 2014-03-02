(ns nicocrawler-clj.ffmpeg
(:use [clojure.java.shell :only (sh)]))

(defn flv->mp3 [filename]
  (sh "sh" "-c" (str "ffmpeg -i \"" filename ".flv\" -acodec copy \"" filename ".mp3\"")))

(defn mp4->m4a [filename]
  (sh "sh" "-c" (str "ffmpeg -i \"" filename ".mp4\" -vn -acodec copy \"" filename ".m4a\"")))

(defn swf->mp3 [filename]
  (sh "sh" "-c" (str "swfextract -m \"" filename ".swf\" -o \"" filename ".mp3\"")))
