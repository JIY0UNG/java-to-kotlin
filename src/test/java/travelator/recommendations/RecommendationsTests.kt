package travelator.recommendations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import travelator.Id
import travelator.destinations.FeaturedDestination
import travelator.domain.Location
import kotlin.reflect.KProperty

class Given<T>(private var getter: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getter()
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T
    ) {
        this.getter = { value }
    }
}

class RecommendationsTests {
    var featuredDestinations by Given {
        listOf(
            paris to listOf(eiffelTower, louvre),
        )
    }

    var destinationFinder by Given {
        featuredDestinations.toMap().withDefault { emptyList() }
    }

    var featuredDistances by Given {
        distances.withDefault { -1 }
    }

    var journeys by Given {
        listOf(paris)
    }

    var journey by Given {
        journeys.toSet()
    }

    companion object {
        val distances = mapOf(
            (paris to eiffelTower.location) to 5000,
            (paris to louvre.location) to 1000,
            (alton to flowerFarm.location) to 5300,
            (alton to watercressLine.location) to 320,
            (froyle to flowerFarm.location) to 0,
            (froyle to watercressLine.location) to 6300
        )
    }

    @Nested
    inner class Describe_Recommendation {
        @Nested
        inner class When_no_locations {
            @BeforeEach
            fun setUp() {
                featuredDestinations = emptyList()
                journeys = emptyList()
            }

            @Test
            fun returns_no_recommendations() {
                val recommendations = Recommendations(destinationFinder::getValue, featuredDistances::getValue)
                val suggestions = recommendations.recommendationsFor(journey)

                assertEquals(suggestions, emptyList<FeaturedDestinationSuggestion>())
            }
        }

        @Nested
        inner class When_no_featured {
            @BeforeEach
            fun setUp() {
                featuredDestinations = emptyList()
                journeys = listOf(paris)
            }

            @Test
            fun returns_no_recommendations() {
                val recommendations = Recommendations(destinationFinder::getValue, featuredDistances::getValue)
                val suggestions = recommendations.recommendationsFor(journey)

                assertEquals(suggestions, emptyList<FeaturedDestinationSuggestion>())
            }
        }

        @Nested
        inner class When_single_location {
            @BeforeEach
            fun setUp() {
                featuredDestinations = listOf(
                    paris to listOf(eiffelTower, louvre),
                )
                journeys = listOf(paris, alton)
            }

            @Test
            fun returns_recommendations() {

                val recommendations = Recommendations(destinationFinder::getValue, featuredDistances::getValue)
                val suggestions = recommendations.recommendationsFor(journey)

                assertEquals(
                    suggestions, listOf(
                        FeaturedDestinationSuggestion(paris, louvre, 1000),
                        FeaturedDestinationSuggestion(paris, eiffelTower, 5000)
                    )
                )
            }
        }


        @Test
        fun returns_recommendations_for_multi_location() {
            featuredDestinations = listOf(
                paris to listOf(eiffelTower, louvre),
                alton to listOf(flowerFarm, watercressLine),
            )
            journeys = listOf(paris, alton)

            val recommendations = Recommendations(
                destinationFinder::getValue,
                featuredDistances::getValue
            )
            val suggestions = recommendations.recommendationsFor(journey)

            assertEquals(
                suggestions, listOf(
                    FeaturedDestinationSuggestion(alton, watercressLine, 320),
                    FeaturedDestinationSuggestion(paris, louvre, 1000),
                    FeaturedDestinationSuggestion(paris, eiffelTower, 5000),
                    FeaturedDestinationSuggestion(alton, flowerFarm, 5300)
                )
            )
        }

        @Test
        fun deduplicates_using_smallest_distance() {
            featuredDestinations = listOf(
                alton to listOf(flowerFarm, watercressLine),
                froyle to listOf(flowerFarm, watercressLine)
            )
            journeys = listOf(alton, froyle)

            val recommendations = Recommendations(
                destinationFinder::getValue,
                featuredDistances::getValue
            )
            val suggestions = recommendations.recommendationsFor(journey)

            assertEquals(
                suggestions, listOf(
                    FeaturedDestinationSuggestion(froyle, flowerFarm, 0),
                    FeaturedDestinationSuggestion(alton, watercressLine, 320)
                )
            )
        }
    }
}

private fun <K1, K2, V> Map<Pair<K1, K2>, V>.getValue(k1: K1, k2: K2) =
    getValue(k1 to k2)

private val paris = location("Paris")
private val louvre = featured("Louvre", "Rue de Rivoli")
private val eiffelTower = featured("Eiffel Tower", "Champ de Mars")
private val alton = location("Alton")
private val froyle = location("Froyle")
private val watercressLine = featured("Watercress Line", "Alton Station")
private val flowerFarm = featured("West End Flower Farm", froyle)

private fun location(name: String) = Location(Id.of(name), name, name)

private fun featured(name: String, locationName: String) =
    featured(name, location(locationName))

private fun featured(name: String, location: Location) =
    FeaturedDestination(name, location)