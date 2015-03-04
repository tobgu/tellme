(ns short_report.migrations
  (import [org.flywaydb.core Flyway])
  (require [clojure.java.jdbc :refer [db-do-commands]]))


(defn get-db-datasource []
  (doto (org.postgresql.ds.PGSimpleDataSource.)
        (.setServerName "localhost")
        (.setPortNumber 5434)
        (.setDatabaseName "tellme")
        (.setUser "tellme")
        (.setPassword "letmein")))

(defn migrate []
  (let [datasource (get-db-datasource)
        flyway (doto (Flyway.)
                 (.setDataSource datasource)
                 (.setSqlMigrationPrefix ""))]
	(.migrate flyway)))