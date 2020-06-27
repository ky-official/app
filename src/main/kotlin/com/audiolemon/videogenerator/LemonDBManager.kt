package com.audiolemon.videogenerator

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

/*
* Manages task entries in the database.
* Does CRUD operations needed for logging and monitoring task progress
* */
sealed class LemonDBManager() {

    companion object {

        private lateinit var conn: Connection
        private lateinit var statement: Statement
        private const val URL = "jdbc:h2:./db/LemonDatabase"
        private const val DRIVER = "org.h2.Driver"

        fun connect() {
            try {
                Class.forName(DRIVER)
                conn = DriverManager.getConnection(URL)
                statement = conn.createStatement()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun close() {
            try {
                conn.close()
                statement.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        fun addTask(id: String) {
            this.connect()
            try {
                statement.execute("INSERT INTO TASKS VALUES ('$id','QUEUED',0,CURRENT_TIMESTAMP())")
                println("New task added with id:$id")
                this.close()
            } catch (e: SQLException) {
                this.close()
                e.printStackTrace()
            }
            this.close()
        }

        fun removeTask(id: String) {
            this.connect()
            try {
                statement.execute("DELETE FROM TASKS WHERE ID='$id'")
                println("task removed with id:$id")
                this.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                this.close()
            }
            this.close()
        }

        fun getProgress(id: String): Int {
            this.connect()

            try {
                val result = statement.executeQuery("SELECT PROGRESS FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return result.getInt("PROGRESS")
                    break
                }
                this.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                this.close()
            }
            this.close()
            return 0
        }

        fun getStatus(id: String): String {
            this.connect()
            try {
                val result = statement.executeQuery("SELECT STATUS FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return result.getString("STATUS")
                    break
                }
            } catch (e: SQLException) {
                this.close()
                return "NONE"
            }
            this.close()
            return "NONE"
        }

        fun getTaskStartDate(id: String): String {
            this.connect()
            try {
                val result = statement.executeQuery("SELECT MODIFIED FROM TASKS WHERE ID='$id'")
                while (result.next()) {
                    return result.getString("MODIFIED")
                    break
                }
                this.close()
            } catch (e: SQLException) {
                this.close()
                e.printStackTrace()
            }
            this.close()
            return "NONE"
        }

        fun updateProgress(id: String, percentage: Int) {
            this.connect()
            try {
                statement.executeUpdate("UPDATE TASKS SET PROGRESS=$percentage WHERE ID='$id'")
                this.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                this.close()
            }
            this.close()
        }

        fun updateStatus(id: String, status: String) {
            this.connect()
            try {
                statement.executeUpdate("UPDATE TASKS SET STATUS='$status' WHERE ID='$id'")
                this.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                this.close()
            }
            this.close()
        }
    }

}
