(ns fulcro.client.util-spec
  (:require
    [fulcro-spec.core :refer [specification when-mocking assertions behavior]]
    [fulcro.client.util :as util]
    [om.next :as om]))

(specification "Log app state"
  (let [state (atom {:foo        {:a :b
                                  12 {:c         ["hello" "world"]
                                      [:wee :ha] {:e [{:e :g}
                                                      {:a [1 2 3 4]}
                                                      {:t :k}]
                                                  :g :h
                                                  :i :j}}}
                     {:map :key} {:other :data}
                     [1 2 3]     :data})]

    (when-mocking
      (om/app-state _) => state
      (cljs.pprint/pprint data) => data

      (assertions
        "Handle non-sequential keys"
        (util/log-app-state state {:map :key}) => {:other :data}

        "Handles sequential keys"
        (util/log-app-state state [[1 2 3]]) => :data

        "Handles non-sequential and sequential keys together"
        (util/log-app-state state [:foo :a] {:map :key}) => {:foo        {:a :b}
                                                             {:map :key} {:other :data}}

        "Handles distinct paths"
        (util/log-app-state state [:foo 12 [:wee :ha] :g] [{:map :key}]) => {:foo        {12 {[:wee :ha] {:g :h}}}
                                                                             {:map :key} {:other :data}}

        "Handles shared paths"
        (util/log-app-state state [:foo 12 [:wee :ha] :g] [:foo :a]) => {:foo {12 {[:wee :ha] {:g :h}}
                                                                               :a :b}}

        "Handles keys and paths together"
        (util/log-app-state state {:map :key} [:foo 12 :c 1]) => {:foo        {12 {:c {1 "world"}}}
                                                                  {:map :key} {:other :data}}))))

(specification "strip-parameters"
  (behavior "removes all parameters from"
    (assertions
      "parameterized prop reads"
      (util/strip-parameters `[(:some/key {:arg :foo})]) => [:some/key]

      "parameterized join reads"
      (util/strip-parameters `[({:some/key [:sub/key]} {:arg :foo})]) => [{:some/key [:sub/key]}]

      "nested parameterized join reads"
      (util/strip-parameters
        `[{:some/key [({:sub/key [:sub.sub/key]} {:arg :foo})]}]) => [{:some/key [{:sub/key [:sub.sub/key]}]}]

      "multiple parameterized reads"
      (util/strip-parameters
        `[(:some/key {:arg :foo})
          :another/key
          {:non-parameterized [:join]}
          {:some/other [{:nested [(:parameterized {:join :just-for-fun})]}]}])
      =>
      [:some/key :another/key {:non-parameterized [:join]} {:some/other [{:nested [:parameterized]}]}]

      "parameterized mutations"
      (util/strip-parameters ['(fire-missiles! {:arg :foo})]) => '[fire-missiles!]

      "multiple parameterized mutations"
      (util/strip-parameters ['(fire-missiles! {:arg :foo})
                           '(walk-the-plank! {:right :now})]) => '[fire-missiles! walk-the-plank!])))