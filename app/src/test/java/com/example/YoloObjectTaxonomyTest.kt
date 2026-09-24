package com.example

import com.example.core.model.ObjectCategory
import com.example.core.model.YoloObjectTaxonomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloObjectTaxonomyTest {

    @Test
    fun testCarpetIsNotAnimal() {
        val meta = YoloObjectTaxonomy.resolveLabel("carpet")
        assertEquals(ObjectCategory.FURNITURE, meta.category)
        assertFalse(meta.isLivingBeing)
        assertEquals("Carpet", meta.englishName)

        val metaRug = YoloObjectTaxonomy.resolveLabel("rug")
        assertEquals(ObjectCategory.FURNITURE, metaRug.category)
        assertFalse(metaRug.isLivingBeing)

        val metaMat = YoloObjectTaxonomy.resolveLabel("doormat")
        assertEquals(ObjectCategory.FURNITURE, metaMat.category)
        assertFalse(metaMat.isLivingBeing)
    }

    @Test
    fun testMicrowaveIsNotBirdOrAnimal() {
        val meta = YoloObjectTaxonomy.resolveLabel("microwave")
        assertEquals(ObjectCategory.ELECTRONICS, meta.category)
        assertFalse(meta.isLivingBeing)
        assertEquals("Microwave", meta.englishName)

        val metaOven = YoloObjectTaxonomy.resolveLabel("microwave oven")
        assertEquals(ObjectCategory.ELECTRONICS, metaOven.category)
        assertFalse(metaOven.isLivingBeing)
    }

    @Test
    fun testRefrigeratorIsNotRatOrAnimal() {
        val meta = YoloObjectTaxonomy.resolveLabel("refrigerator")
        assertEquals(ObjectCategory.ELECTRONICS, meta.category)
        assertFalse(meta.isLivingBeing)
        assertEquals("Refrigerator", meta.englishName)

        val metaFridge = YoloObjectTaxonomy.resolveLabel("fridge")
        assertEquals(ObjectCategory.ELECTRONICS, metaFridge.category)
        assertFalse(metaFridge.isLivingBeing)
    }

    @Test
    fun testKitchenwareIsNotHenOrBird() {
        val meta = YoloObjectTaxonomy.resolveLabel("kitchenware")
        assertEquals(ObjectCategory.EVERYDAY, meta.category)
        assertFalse(meta.isLivingBeing)

        val metaUtensil = YoloObjectTaxonomy.resolveLabel("utensil")
        assertEquals(ObjectCategory.EVERYDAY, metaUtensil.category)
        assertFalse(metaUtensil.isLivingBeing)
    }

    @Test
    fun testDoorHandleIsNotHumanOrHand() {
        val meta = YoloObjectTaxonomy.resolveLabel("door handle")
        assertEquals(ObjectCategory.ENVIRONMENT, meta.category)
        assertFalse(meta.isLivingBeing)

        val metaHandle = YoloObjectTaxonomy.resolveLabel("handle")
        assertEquals(ObjectCategory.ENVIRONMENT, metaHandle.category)
        assertFalse(metaHandle.isLivingBeing)
    }

    @Test
    fun testBatteryIsNotBatOrAnimal() {
        val meta = YoloObjectTaxonomy.resolveLabel("battery")
        assertEquals(ObjectCategory.ELECTRONICS, meta.category)
        assertFalse(meta.isLivingBeing)

        val metaCharger = YoloObjectTaxonomy.resolveLabel("phone charger")
        assertEquals(ObjectCategory.ELECTRONICS, metaCharger.category)
        assertFalse(metaCharger.isLivingBeing)
    }

    @Test
    fun testCommonHouseholdNonLivingItems() {
        val chair = YoloObjectTaxonomy.resolveLabel("chair")
        assertEquals(ObjectCategory.FURNITURE, chair.category)
        assertFalse(chair.isLivingBeing)

        val table = YoloObjectTaxonomy.resolveLabel("table")
        assertEquals(ObjectCategory.FURNITURE, table.category)
        assertFalse(table.isLivingBeing)

        val bottle = YoloObjectTaxonomy.resolveLabel("water bottle")
        assertEquals(ObjectCategory.EVERYDAY, bottle.category)
        assertFalse(bottle.isLivingBeing)

        val laptop = YoloObjectTaxonomy.resolveLabel("laptop")
        assertEquals(ObjectCategory.ELECTRONICS, laptop.category)
        assertFalse(laptop.isLivingBeing)

        val cup = YoloObjectTaxonomy.resolveLabel("cup")
        assertEquals(ObjectCategory.EVERYDAY, cup.category)
        assertFalse(cup.isLivingBeing)
    }

    @Test
    fun testGenuineLivingBeings() {
        val dog = YoloObjectTaxonomy.resolveLabel("dog")
        assertEquals(ObjectCategory.ANIMAL, dog.category)
        assertTrue(dog.isLivingBeing)

        val cat = YoloObjectTaxonomy.resolveLabel("cat")
        assertEquals(ObjectCategory.ANIMAL, cat.category)
        assertTrue(cat.isLivingBeing)

        val cow = YoloObjectTaxonomy.resolveLabel("cow")
        assertEquals(ObjectCategory.ANIMAL, cow.category)
        assertTrue(cow.isLivingBeing)

        val person = YoloObjectTaxonomy.resolveLabel("person")
        assertEquals(ObjectCategory.PERSON, person.category)
        assertTrue(person.isLivingBeing)

        val child = YoloObjectTaxonomy.resolveLabel("child")
        assertEquals(ObjectCategory.PERSON, child.category)
        assertTrue(child.isLivingBeing)
    }

    @Test
    fun testFloraPlantsAndTrees() {
        val plant = YoloObjectTaxonomy.resolveLabel("potted plant")
        assertEquals(ObjectCategory.ENVIRONMENT, plant.category)
        assertTrue(plant.isFlora)
        assertTrue(plant.isLivingBeing)

        val tree = YoloObjectTaxonomy.resolveLabel("tree")
        assertEquals(ObjectCategory.ENVIRONMENT, tree.category)
        assertTrue(tree.isFlora)
        assertTrue(tree.isLivingBeing)
    }

    @Test
    fun testPlurals() {
        val chairs = YoloObjectTaxonomy.resolveLabel("chairs")
        assertEquals(ObjectCategory.FURNITURE, chairs.category)
        assertEquals("Chair", chairs.englishName)

        val bottles = YoloObjectTaxonomy.resolveLabel("bottles")
        assertEquals(ObjectCategory.EVERYDAY, bottles.category)
        assertEquals("Bottle", bottles.englishName)
    }
}
