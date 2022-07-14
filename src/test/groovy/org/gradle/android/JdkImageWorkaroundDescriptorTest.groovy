package org.gradle.android

import com.google.common.collect.Sets
import org.gradle.android.workarounds.JdkImageWorkaround
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.module.ModuleDescriptor

class JdkImageWorkaroundDescriptorTest extends Specification {
    def "normalizes module descriptors with different orders in the values"() {
        given:
        def serialized1 = JdkImageWorkaround.ExtractJdkImageTransform.serializeDescriptor(descriptor(["org.foo", "org.bar"], ["baz", "fizz"]))
        def serialized2 = JdkImageWorkaround.ExtractJdkImageTransform.serializeDescriptor(descriptor(["org.bar", "org.foo"], ["fizz", "baz"]))

        expect:
        serialized1 == serialized2
    }

    def "descriptors with different values are serialized differently"() {
        given:
        def serialized1 = JdkImageWorkaround.ExtractJdkImageTransform.serializeDescriptor(descriptor(["org.foo", "org.bar"], ["baz", "fizz"]))
        def serialized2 = JdkImageWorkaround.ExtractJdkImageTransform.serializeDescriptor(descriptor(["org.bar", "org.foo", "org.baz"], ["baz", "fizz"]))

        expect:
        serialized1 != serialized2
    }

    @Unroll
    def "all descriptor values are captured (#modifier.trim())"() {
        ModuleDescriptor descriptor = builder
            .requires("foo").requires("bar")
            .uses("org.baz").uses("org.fizz")
            .exports("org.exports", ["exportsTarget1", "exportsTarget2"] as Set)
            .provides("org.provides", ["org.provider1", "org.provider2"])
            .with {
                modifier == "" ? opens("org.opens", ["opensTarget1", "opensTarget2"] as Set) : it
            }.build()

        expect:
        JdkImageWorkaround.ExtractJdkImageTransform.serializeDescriptor(descriptor) == "${modifier}module { name: myModule, " +
            "[bar, foo, mandated java.base], " +
            "uses: [org.baz, org.fizz], " +
            "exports: [org.exports to [exportsTarget1, exportsTarget2]], " +
            (modifier == "" ? "opens: [org.opens to [opensTarget1, opensTarget2]], " : "") +
            "provides: [org.provides with [org.provider1, org.provider2]] }"

        where:
        builder                                    | modifier
        ModuleDescriptor.newModule("myModule")     | ""
        ModuleDescriptor.newOpenModule("myModule") | "open "
    }

    static def descriptor(List<String> packages, List<String> values) {
        ModuleDescriptor.Builder builder = ModuleDescriptor.newModule("myModule")
        values.each { builder.requires(it) }
        packages.each { builder.uses(it) }
        packages.each { builder.exports("${it}.exports", Sets.newLinkedHashSet(values)) }
        packages.each { builder.opens("${it}.opens", Sets.newLinkedHashSet(values)) }
        packages.each { builder.provides("${it}.provides", values.collect { v -> "org.${v}".toString() }) }
        return builder.build()
    }
}
