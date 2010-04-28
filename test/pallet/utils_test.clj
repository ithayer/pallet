(ns pallet.utils-test
  (:use [pallet.utils] :reload-all)
  (:use clojure.test
        clojure.contrib.logging
        pallet.test-utils))

(deftest system-test
  (cond
   (.canRead (java.io.File. "/usr/bin/true")) (is (= {:exit 0 :out "" :err ""}
                                                     (system "/usr/bin/true")))
   (.canRead (java.io.File. "/bin/true")) (is (= {:exit 0 :out "" :err ""}
                                                 (system "/bin/true")))
   :else (warn "Skipping system-test")))

(deftest bash-test
  (is (= {:exit 0 :out "fred\n" :err ""} (bash "echo fred"))))

(deftest make-user-test
  (let [username "userfred"
        password "pw"
        private-key-path "pri"
        public-key-path "pub"]
    (is (= {:username username
            :password password
            :private-key-path private-key-path
            :public-key-path public-key-path
            :sudo-password password
            :no-sudo nil}
           (make-user username
                      :password password
                      :private-key-path private-key-path
                      :public-key-path public-key-path)))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :sudo-password nil
            :no-sudo nil}
           (make-user username)))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :sudo-password password
            :no-sudo nil}
           (make-user username :sudo-password password)))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :sudo-password nil
            :no-sudo true}
           (make-user username :no-sudo true)))))

(deftest sudo-cmd-for-test
  (let [no-pw "/usr/bin/sudo -n"
        pw "echo \"fred\" | /usr/bin/sudo -S"
        no-sudo ""]
    (is (= no-pw (sudo-cmd-for (make-user "fred"))))
    (is (= pw (sudo-cmd-for (make-user "fred" :password "fred"))))
    (is (= pw (sudo-cmd-for (make-user "fred" :sudo-password "fred"))))
    (is (= no-pw
           (sudo-cmd-for (make-user "fred" :password "fred" :sudo-password false))))
    (is (= no-sudo (sudo-cmd-for (make-user "root"))))
    (is (= no-sudo (sudo-cmd-for (make-user "fred" :no-sudo true))))))

(deftest sh-script-test
  (let [res (sh-script
             "file=$(mktemp utilXXXX); echo fred > $file ;cat $file ; rm $file")]
    (is (= {:exit 0 :err "" :out "fred\n"} res))))

(deftest blank?-test
  (is (blank? nil))
  (is (blank? ""))
  (is (not (blank? "a")))
  (is (not (blank? 'a))))

(deftest cmd-join-test
  (is (= "fred\n" (cmd-join ["fred"])))
  (is (= "fred\nblogs\n" (cmd-join ["fred" "blogs"])))
  (is (= "fred\nblogs\n" (cmd-join ["fred\n\n" "blogs\n"]))))

(deftest do-script-test
  (is (= "fred\n" (do-script "fred")))
  (is (= "fred\nblogs\n" (do-script "fred" "blogs")))
  (is (= "fred\nblogs\n" (do-script "fred\n\n" "blogs\n"))))

(deftest cmd-chain-test
  (is (= "fred" (cmd-chain ["fred"])))
  (is (= "fred && blogs" (cmd-chain ["fred" "blogs"])))
  (is (= "fred && blogs" (cmd-chain ["fred\n\n" "blogs\n"]))))

(deftest cmd-join-checked-test
  (is (= "echo \"test...\"\n{ echo fred && echo tom; } || { echo test failed ; exit 1 ; } >&2 \necho \"...done\"\n"
         (cmd-join-checked "test" ["echo fred" "echo tom"])))
  (is (= "test...\nfred\ntom\n...done\n"
         (bash-out (cmd-join-checked "test" ["echo fred" "echo tom"]))))
  (is (= "test...\n"
         (bash-out (cmd-join-checked "test" ["test 1 = 2"]) 1 "test failed\n"))))
