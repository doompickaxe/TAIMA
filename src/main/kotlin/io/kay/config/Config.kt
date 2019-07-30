package io.kay.config

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

class Config {
    companion object {
        fun readDBConfig(): DatabaseConfig {
            val config = ConfigFactory.defaultApplication()
            return config.extract("database")
        }

        fun readAuthConfig(): GoogleAuthConfig {
            val config = ConfigFactory.defaultApplication()
            return config.extract("google-auth")
        }
    }
}