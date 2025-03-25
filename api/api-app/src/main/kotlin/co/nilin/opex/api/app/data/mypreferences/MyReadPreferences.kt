package co.nilin.opex.api.app.data.mypreferences

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class MyReadPreferences(
    @Value("\${preferences.yml.path}") private val preferencesYmlPath: String
) {
    private val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    @Bean
    fun myPreferences(): MyPreferences {
        println("🟢 Đọc file YAML từ: $preferencesYmlPath")
        val file = File(preferencesYmlPath)

        if (!file.exists()) {
            throw RuntimeException("❌ File YAML không tồn tại: $preferencesYmlPath")
        }

        val content = file.readText()
        println("🟡 Nội dung file:\n$content")

        return mapper.readValue(content, MyPreferences::class.java)
    }
}