;; Copyright © 2022 Manetu, Inc.  All rights reserved

(ns temporal.test.client-signal
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [temporal.client.core :refer [>!] :as c]
            [temporal.signals :refer [<!]]
            [temporal.workflow :refer [defworkflow]]
            [temporal.test.utils :as t]))

(use-fixtures :once t/wrap-service)

(def signal-name ::signal)

(defn lazy-signals [signals]
  (lazy-seq (when-let [m (<! signals signal-name)]
              (cons m (lazy-signals signals)))))

(defworkflow test-workflow
  [ctx {:keys [signals] {:keys [nr] :as args} :args}]
  (log/info "test-workflow:" args)
  (doall (take nr (lazy-signals signals))))

(def expected 3)

(deftest the-test
  (testing "Verifies that we can send signals from a client"
    (let [workflow (t/create-workflow test-workflow)]
      (c/start workflow {:nr expected})
      (dotimes [n expected]
        (>! workflow signal-name n))
      (is (-> workflow c/get-result deref count (= expected))))))
