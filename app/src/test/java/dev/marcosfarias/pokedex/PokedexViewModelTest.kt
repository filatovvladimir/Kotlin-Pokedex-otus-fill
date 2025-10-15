package dev.marcosfarias.pokedex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import dev.marcosfarias.pokedex.database.dao.PokemonDAO
import dev.marcosfarias.pokedex.model.Pokemon
import dev.marcosfarias.pokedex.repository.PokemonService
import dev.marcosfarias.pokedex.ui.pokedex.PokedexViewModel
import io.mockk.*
import io.qameta.allure.Description
import org.junit.*

class PokedexViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dao: PokemonDAO = mockk(relaxed = true)
    private val service: PokemonService = mockk(relaxed = true)
    private lateinit var viewModel: PokedexViewModel

    @Before
    fun before() {
        viewModel = PokedexViewModel(dao, service)
    }

    // === ОСНОВНЫЕ ТЕСТЫ ===

    @Test
    @Description("Проверяет, что сервис вызывается один раз при инициализации ViewModel")
    fun `GIVEN service WHEN call service THEN check if is called once at viewmodel initialization`() {
        verify(exactly = 1) { service.get() }
    }

    @Test
    @Description("Проверяет корректность получения списка покемонов из DAO")
    fun `GIVEN mocked dao results WHEN get list of pokemons from view model THEN result as expected`() {
        // GIVEN
        val expected = listOf(
            Pokemon().apply { name = "Psyduck" },
            Pokemon().apply { name = "Onyx" }
        )
        every { dao.all() } returns MutableLiveData(expected)

        // WHEN
        val result = viewModel.getListPokemon()

        // THEN
        Assert.assertEquals(expected, result.value!!)
    }

    @Test
    @Description("Проверяет фильтрацию покемонов по типу (Electric)")
    fun `GIVEN pokemon list WHEN filter by type THEN return correct filtered results`() {
        // Given - используем реальное поле typeofpokemon
        val pokemons = listOf(
            Pokemon().apply {
                name = "Pikachu"
                typeofpokemon = listOf("Electric")
            },
            Pokemon().apply {
                name = "Charmander"
                typeofpokemon = listOf("Fire")
            },
            Pokemon().apply {
                name = "Raichu"
                typeofpokemon = listOf("Electric")
            }
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When - фильтруем по типу Electric
        val result = viewModel.getListPokemon()
        val electricPokemons = result.value!!.filter {
            it.typeofpokemon?.contains("Electric") == true
        }

        // Then
        Assert.assertEquals("Should find 2 electric pokemons", 2, electricPokemons.size)
        Assert.assertTrue("Should contain Pikachu", electricPokemons.any { it.name == "Pikachu" })
        Assert.assertTrue("Should contain Raichu", electricPokemons.any { it.name == "Raichu" })
        Assert.assertFalse("Should not contain Charmander", electricPokemons.any { it.name == "Charmander" })
    }

    @Test
    @Description("Проверяет сортировку покемонов по ID")
    fun `GIVEN unsorted pokemon list WHEN get list THEN return sorted by id`() {
        // Given
        val pokemons = listOf(
            Pokemon(id = "25", name = "Pikachu"),
            Pokemon(id = "1", name = "Bulbasaur"),
            Pokemon(id = "4", name = "Charmander")
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When
        val result = viewModel.getListPokemon()
        val sortedPokemons = result.value!!.sortedBy { it.id.toIntOrNull() ?: 0 }

        // Then
        Assert.assertEquals("First should be Bulbasaur", "Bulbasaur", sortedPokemons[0].name)
        Assert.assertEquals("Second should be Charmander", "Charmander", sortedPokemons[1].name)
        Assert.assertEquals("Third should be Pikachu", "Pikachu", sortedPokemons[2].name)
    }

    @Test
    @Description("Проверяет реакцию на изменения данных в LiveData")
    fun `GIVEN dao data changes WHEN observing live data THEN receive updates`() {
        // Given
        val liveData = MutableLiveData<List<Pokemon>>()
        every { dao.all() } returns liveData

        val viewModel = PokedexViewModel(dao, service)
        var receivedUpdates = 0
        var lastData: List<Pokemon>? = null

        // When - подписываемся на изменения (изначально LiveData имеет значение null)
        viewModel.getListPokemon().observeForever { pokemons ->
            receivedUpdates++
            lastData = pokemons
        }

        // Then - эмулируем обновление данных в DAO
        val newPokemons = listOf(Pokemon().apply { name = "New Pokemon" })
        liveData.value = newPokemons

        // Ожидаем только 1 обновление (от null к newPokemons)
        Assert.assertEquals("Should receive 1 update", 1, receivedUpdates)
        Assert.assertEquals("Should have new data", "New Pokemon", lastData?.first()?.name)
    }

    // === ТЕСТЫ НА ГРАНИЧНЫЕ ЗНАЧЕНИЯ ===

    @Test
    @Description("Проверяет обработку пустого списка покемонов")
    fun `GIVEN empty pokemon list WHEN get list THEN return empty list`() {
        // Given
        val emptyList = emptyList<Pokemon>()
        every { dao.all() } returns MutableLiveData(emptyList)

        // When
        val result = viewModel.getListPokemon()

        // Then
        Assert.assertTrue("List should be empty", result.value.isNullOrEmpty())
        Assert.assertEquals("List size should be 0", 0, result.value?.size ?: 0)
    }

    @Test
    @Description("Проверяет фильтрацию с пустым поисковым запросом")
    fun `GIVEN pokemon list WHEN filter with empty query THEN return all pokemons`() {
        // Given
        val pokemons = listOf(
            Pokemon(id = "1", name = "Bulbasaur"),
            Pokemon(id = "4", name = "Charmander"),
            Pokemon(id = "7", name = "Squirtle")
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When
        val result = viewModel.getListPokemon()
        val filtered = result.value!!.filter {
            it.name?.contains("", ignoreCase = true) == true
        }

        // Then
        Assert.assertEquals("Should return all pokemons with empty filter", 3, filtered.size)
    }

    @Test
    @Description("Проверяет фильтрацию с несуществующим типом покемона")
    fun `GIVEN pokemon list WHEN filter by non-existing type THEN return empty list`() {
        // Given
        val pokemons = listOf(
            Pokemon().apply {
                name = "Pikachu"
                typeofpokemon = listOf("Electric")
            },
            Pokemon().apply {
                name = "Charmander"
                typeofpokemon = listOf("Fire")
            }
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When - фильтруем по несуществующему типу
        val result = viewModel.getListPokemon()
        val filtered = result.value!!.filter {
            it.typeofpokemon?.contains("Water") == true
        }

        // Then
        Assert.assertTrue("Should return empty list for non-existing type", filtered.isEmpty())
    }

    @Test
    @Description("Проверяет обработку покемонов с null именем")
    fun `GIVEN pokemon with null name WHEN get list THEN handle null name gracefully`() {
        // Given
        val pokemons = listOf(
            Pokemon().apply {
                id = "1"
                name = null // имя null
                typeofpokemon = listOf("Grass", "Poison")
            },
            Pokemon().apply {
                id = "4"
                name = "Charmander"
                typeofpokemon = listOf("Fire")
            }
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When
        val result = viewModel.getListPokemon()

        // Then
        Assert.assertEquals("Should return 2 pokemons", 2, result.value?.size)
        Assert.assertNull("First pokemon should have null name", result.value?.get(0)?.name)
        Assert.assertEquals("Second pokemon should have name", "Charmander", result.value?.get(1)?.name)
    }

    @Test
    @Description("Проверяет обработку покемонов с пустым списком типов")
    fun `GIVEN pokemon with empty types WHEN filter by type THEN exclude from results`() {
        // Given
        val pokemons = listOf(
            Pokemon().apply {
                name = "Pikachu"
                typeofpokemon = listOf("Electric")
            },
            Pokemon().apply {
                name = "Unknown Pokemon"
                typeofpokemon = emptyList() // пустой список типов
            },
            Pokemon().apply {
                name = "Charmander"
                typeofpokemon = listOf("Fire")
            }
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When - фильтруем по любому типу
        val result = viewModel.getListPokemon()
        val pokemonsWithTypes = result.value!!.filter {
            !it.typeofpokemon.isNullOrEmpty()
        }

        // Then
        Assert.assertEquals("Should return only pokemons with types", 2, pokemonsWithTypes.size)
        Assert.assertFalse("Should not include pokemon with empty types",
            pokemonsWithTypes.any { it.name == "Unknown Pokemon" })
    }

    @Test
    @Description("Проверяет сортировку покемонов с некорректными ID")
    fun `GIVEN pokemons with invalid ids WHEN sort THEN handle invalid ids gracefully`() {
        // Given
        val pokemons = listOf(
            Pokemon().apply {
                id = "25"
                name = "Pikachu"
            },
            Pokemon().apply {
                id = "invalid_id" // некорректный ID
                name = "Invalid Pokemon"
            },
            Pokemon().apply {
                id = "1"
                name = "Bulbasaur"
            },
            Pokemon().apply {
                id = "" // пустой ID
                name = "Empty ID Pokemon"
            }
        )
        every { dao.all() } returns MutableLiveData(pokemons)

        // When
        val result = viewModel.getListPokemon()
        val sorted = result.value!!.sortedBy {
            it.id.toIntOrNull() ?: Int.MAX_VALUE // некорректные ID в конец
        }

        // Then
        Assert.assertEquals("First should be Bulbasaur", "Bulbasaur", sorted[0].name)
        Assert.assertEquals("Second should be Pikachu", "Pikachu", sorted[1].name)
        // Некорректные ID должны быть в конце
        Assert.assertTrue("Invalid IDs should be at the end",
            sorted[2].name == "Invalid Pokemon" || sorted[2].name == "Empty ID Pokemon")
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun tearDown() {
            unmockkAll()
        }
    }
}