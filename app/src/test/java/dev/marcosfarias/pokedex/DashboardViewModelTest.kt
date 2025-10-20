package dev.marcosfarias.pokedex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.marcosfarias.pokedex.database.dao.PokemonDAO
import dev.marcosfarias.pokedex.model.Pokemon
import dev.marcosfarias.pokedex.ui.dashboard.DashboardViewModel
import io.mockk.*
import io.qameta.allure.Description
import org.junit.*

class DashboardViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dao: PokemonDAO = mockk(relaxed = true)
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun before() {
        viewModel = DashboardViewModel(pokemonDAO = dao)
    }

    @Test
    @Description("Проверяет получение покемона по ID")
    fun `GIVEN id WHEN request pokemon THEN get pokemon as result and call only 1 request`() {
        // GIVEN
        val input = "123"
        val expected: LiveData<Pokemon> = mockk(relaxed = true)
        every { dao.getById(input) } returns expected

        // WHEN
        val result = viewModel.getPokemonById(id = input)

        // THEN
        verify(exactly = 1) { dao.getById(input) }
        confirmVerified(dao)
        Assert.assertEquals(expected.value, result.value)
    }

    @Test
    @Description("Проверяет получение списка эволюций покемона")
    fun `GIVEN a list of ids WHEN request pokemon evolutions THEN get a list of pokemons and call only 1 request`() {
        // GIVEN
        val input = listOf("123", "321")
        val pikachu = Pokemon().apply { name = "Pikachu" }
        val squirtle = Pokemon().apply { name = "Squirtle" }
        val expected = MutableLiveData(listOf(pikachu, squirtle))
        every { dao.getEvolutionsByIds(input) } returns expected

        // WHEN
        val result = viewModel.getPokemonEvolutionsByIds(ids = input)

        // THEN
        verify(exactly = 1) { dao.getEvolutionsByIds(input) }
        confirmVerified(dao)
        Assert.assertEquals(expected, result)
    }

    @Test
    @Description("Проверяет получение покемона по ID с корректными данными")
    fun `GIVEN DashboardViewModel WHEN get pokemon by id THEN return correct pokemon`() {
        // Given
        val pokemonId = "25"
        val expectedPokemon = Pokemon().apply {
            id = pokemonId
            name = "Pikachu"
            typeofpokemon = listOf("Electric")
        }

        val liveData = MutableLiveData<Pokemon>()
        liveData.value = expectedPokemon

        every { dao.getById(pokemonId) } returns liveData

        // When
        val result = viewModel.getPokemonById(pokemonId)

        // Then
        verify(exactly = 1) { dao.getById(pokemonId) }
        Assert.assertEquals("Should return Pikachu", "Pikachu", result.value?.name)
        Assert.assertEquals("Should have Electric type", listOf("Electric"), result.value?.typeofpokemon)
    }

    @Test
    @Description("Проверяет получение эволюций покемона с корректными данными")
    fun `GIVEN DashboardViewModel WHEN get pokemon evolutions THEN return evolution list`() {
        // Given
        val evolutionIds = listOf("133", "134", "135")
        val evolutions = listOf(
            Pokemon().apply { id = "133"; name = "Eevee" },
            Pokemon().apply { id = "134"; name = "Vaporeon" },
            Pokemon().apply { id = "135"; name = "Jolteon" }
        )

        val liveData = MutableLiveData(evolutions)
        every { dao.getEvolutionsByIds(evolutionIds) } returns liveData

        // When
        val result = viewModel.getPokemonEvolutionsByIds(evolutionIds)

        // Then
        verify(exactly = 1) { dao.getEvolutionsByIds(evolutionIds) }
        Assert.assertEquals("Should return 3 evolutions", 3, result.value?.size)
        Assert.assertTrue("Should contain Eevee", result.value!!.any { it.name == "Eevee" })
        Assert.assertTrue("Should contain Vaporeon", result.value!!.any { it.name == "Vaporeon" })
        Assert.assertTrue("Should contain Jolteon", result.value!!.any { it.name == "Jolteon" })
    }

    @Test
    @Description("Проверяет обработку пустого списка эволюций")
    fun `GIVEN DashboardViewModel WHEN get empty evolutions THEN return empty list`() {
        // Given
        val emptyEvolutionIds = emptyList<String>()
        val emptyEvolutions = MutableLiveData(emptyList<Pokemon>())

        every { dao.getEvolutionsByIds(emptyEvolutionIds) } returns emptyEvolutions

        // When
        val result = viewModel.getPokemonEvolutionsByIds(emptyEvolutionIds)

        // Then
        Assert.assertTrue("Should return empty list", result.value.isNullOrEmpty())
    }

    @Test
    @Description("Проверяет обработку несуществующего покемона")
    fun `GIVEN DashboardViewModel WHEN get non-existing pokemon THEN return null`() {
        // Given
        val nonExistingId = "9999"
        val nullLiveData = MutableLiveData<Pokemon>()
        nullLiveData.value = null

        every { dao.getById(nonExistingId) } returns nullLiveData

        // When
        val result = viewModel.getPokemonById(nonExistingId)

        // Then
        Assert.assertNull("Should return null for non-existing pokemon", result.value)
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun tearDown() {
            unmockkAll()
        }
    }
}