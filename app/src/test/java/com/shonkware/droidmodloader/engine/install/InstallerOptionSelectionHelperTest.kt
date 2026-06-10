package com.shonkware.droidmodloader.engine.install

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals


class InstallerOptionSelectionHelperTest {

    @Test
    fun selectExactlyOne_replacesExistingSelectionInGroup() {
        val group = group(
            type = InstallerGroupType.SELECT_EXACTLY_ONE,
            options = listOf(option("a"), option("b"), option("c"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a", "outside"),
            optionId = "b"
        )

        assertEquals(setOf("b", "outside"), result)
    }

    @Test
    fun selectAtMostOne_deselectsSelectedOption() {
        val group = group(
            type = InstallerGroupType.SELECT_AT_MOST_ONE,
            options = listOf(option("a"), option("b"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a", "outside"),
            optionId = "a"
        )

        assertEquals(setOf("outside"), result)
    }

    @Test
    fun selectAtMostOne_replacesExistingSelectionWhenSelectingDifferentOption() {
        val group = group(
            type = InstallerGroupType.SELECT_AT_MOST_ONE,
            options = listOf(option("a"), option("b"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a", "outside"),
            optionId = "b"
        )

        assertEquals(setOf("b", "outside"), result)
    }

    @Test
    fun selectAtLeastOne_doesNotDeselectLastSelectedOption() {
        val group = group(
            type = InstallerGroupType.SELECT_AT_LEAST_ONE,
            options = listOf(option("a"), option("b"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a"),
            optionId = "a"
        )

        assertEquals(setOf("a"), result)
    }

    @Test
    fun selectAtLeastOne_deselectsOptionWhenAnotherOptionRemainsSelected() {
        val group = group(
            type = InstallerGroupType.SELECT_AT_LEAST_ONE,
            options = listOf(option("a"), option("b"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a", "b"),
            optionId = "a"
        )

        assertEquals(setOf("b"), result)
    }

    @Test
    fun selectAny_togglesOptionOnAndOff() {
        val group = group(
            type = InstallerGroupType.SELECT_ANY,
            options = listOf(option("a"), option("b"))
        )

        val selected = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = emptySet(),
            optionId = "a"
        )

        assertEquals(setOf("a"), selected)

        val deselected = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = selected,
            optionId = "a"
        )

        assertEquals(emptySet<String>(), deselected)
    }

    @Test
    fun requiredOptionStaysSelected() {
        val group = group(
            type = InstallerGroupType.SELECT_ANY,
            options = listOf(option("required", required = true))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = emptySet(),
            optionId = "required"
        )

        assertEquals(setOf("required"), result)
    }

    @Test
    fun unknownOptionKeepsSelectionUnchanged() {
        val group = group(
            type = InstallerGroupType.SELECT_ANY,
            options = listOf(option("a"))
        )

        val result = InstallerOptionSelectionHelper.toggleOption(
            groups = listOf(group),
            selectedOptionIds = setOf("a"),
            optionId = "missing"
        )

        assertEquals(setOf("a"), result)
    }

    private fun group(
        type: InstallerGroupType,
        id: String = "group",
        options: List<InstallerOption>
    ): InstallerGroup {
        return InstallerGroup(
            id = id,
            name = id,
            type = type,
            options = options
        )
    }

    private fun option(
        id: String,
        required: Boolean = false
    ): InstallerOption {
        return InstallerOption(
            id = id,
            name = id,
            sourcePath = id,
            required = required
        )
    }
}