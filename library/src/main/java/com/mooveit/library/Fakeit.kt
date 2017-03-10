package com.mooveit.library

import android.content.Context
import com.mooveit.library.providers.AddressProviderImpl
import com.mooveit.library.providers.BusinessProviderImpl
import com.mooveit.library.providers.CardProviderImpl
import com.mooveit.library.providers.NameProviderImpl
import com.mooveit.library.providers.definition.AddressProvider
import com.mooveit.library.providers.definition.BusinessProvider
import com.mooveit.library.providers.definition.CardProvider
import com.mooveit.library.providers.definition.NameProvider
import org.yaml.snakeyaml.Yaml
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Fakeit private constructor(context: Context, locale: Locale) {

    val numeralAndBracesRegEx = "#\\{(.*?)\\}"
    val numeralRegEx = ".*#(\\{[^a-zA-z]|[^{])+"
    val numeralOnlyRegEx = "#"
    val yaml = Yaml()
    val values: LinkedHashMap<String, LinkedHashMap<String, String>>

    init {
        val assetManager = context.assets

        val inputStream = assetManager.open(("locales/".plus(locale.language.plus(".yml"))))
        val yamlValues = yaml.load(inputStream) as Map<String, Map<String, String>>
        val localeValues = yamlValues.get(locale.language) as Map<String, Map<String, String>>
        this.values = localeValues.get("faker") as LinkedHashMap<String, LinkedHashMap<String, String>>
    }

    fun fetchCategoryValues(category: String): LinkedHashMap<*, *> {
        var separator = category.indexOf(".")
        var subCategory = category
        var values = this.values
        while (separator != -1) {
            values = values[subCategory.substring(0, separator)] as LinkedHashMap<String, LinkedHashMap<String, String>>
            subCategory = subCategory.substring(separator + 1, subCategory.length)
            separator = subCategory.indexOf(".")
        }
        return values[subCategory] as LinkedHashMap<*, *>
    }

    fun fetch(key: String): String {
        val separator = key.lastIndexOf(".")
        val category = key.substring(0, separator)
        val selected = key.substring(separator + 1, key.length)
        val categoryValues = fetchCategoryValues(category)
        val selectedValues = categoryValues[selected] as ArrayList<String>

        if (selectedValues[0].matches(Regex(numeralAndBracesRegEx))) {
            return getDataToFetch(category, selectedValues)
        } else if (selectedValues[0].matches(Regex(numeralRegEx))) {
            return fetchNumerals(selectedValues)
        } else {
            return getRandomString(selectedValues)
        }
    }

    fun matchAndReplace(stringToMatch: String, stringBuffer: StringBuffer, regExp: String, method: (Matcher) -> Matcher): String {
        val matcher = Pattern.compile(regExp).matcher(stringToMatch)
        while (matcher.find()) {
            method(matcher)
        }
        matcher.appendTail(stringBuffer)
        return stringBuffer.toString()
    }

    fun fetchNumerals(selectedValues: ArrayList<String>): String {
        val numeral = getRandomString(selectedValues)
        val stringBuffer = StringBuffer()
        return matchAndReplace(numeral, stringBuffer, numeralOnlyRegEx,
                { matcher -> matcher.appendReplacement(stringBuffer, Random().nextInt(10).toString()) })
    }

    fun getDataToFetch(category: String, selectedValues: ArrayList<String>): String {
        val setOfValues = getRandomString(selectedValues)
        val stringBuffer = StringBuffer()
        return matchAndReplace(setOfValues, stringBuffer, numeralAndBracesRegEx,
                { matcher -> matcher.appendReplacement(stringBuffer, fetchValueByCategory(category, matcher.group(1))) })
    }

    fun fetchValueByCategory(category: String, key: String): String {
        val separator = key.indexOf(".")
        var dataCategory = category
        var keyToFetch = key
        if (separator != -1) {
            dataCategory = key.substring(0, separator).toLowerCase()
            keyToFetch = key.substring(separator + 1, key.length)
        }
        val categoryValues = this.values[dataCategory] as LinkedHashMap<String, ArrayList<String>>
        val selectedValues = categoryValues[keyToFetch] as ArrayList<String>

        var result = getRandomString(selectedValues)
        if (result.matches(Regex(numeralRegEx))) {
            result = fetchNumerals(selectedValues)
        }
        if (selectedValues[0].matches(Regex(numeralAndBracesRegEx))) {
            result = getDataToFetch(category, selectedValues)
        }
        return result
    }

    fun getRandomString(selectedValues: ArrayList<String>): String {
        return selectedValues[Random().nextInt(selectedValues.size)]
    }

    companion object Companion {

        var fakeit: Fakeit? = null

        fun fakeitInit(context: Context, locale: Locale) {
            if (fakeit == null) {
                synchronized(Fakeit::class.java) {
                    if (fakeit == null) {
                        fakeit = Fakeit(context, locale)
                    }
                }
            }
        }

        @JvmStatic
        fun init(context: Context): Fakeit {
            fakeitInit(context, Locale.getDefault())
            return fakeit as Fakeit
        }

        @JvmStatic
        fun initWithLocale(context: Context, locale: Locale): Fakeit {
            fakeitInit(context, locale)
            return fakeit as Fakeit
        }

        @JvmStatic
        fun initWithLocale(context: Context, localeString: String): Fakeit {
            return initWithLocale(context, Locale(localeString))
        }

        @JvmStatic
        fun name(): NameProvider {
            return NameProviderImpl()
        }

        @JvmStatic
        fun business(): BusinessProvider {
            return BusinessProviderImpl()
        }

        @JvmStatic
        fun address(): AddressProvider {
            return AddressProviderImpl()
        }

        @JvmStatic
        fun card(): CardProvider {
            return CardProviderImpl()
        }
    }
}
