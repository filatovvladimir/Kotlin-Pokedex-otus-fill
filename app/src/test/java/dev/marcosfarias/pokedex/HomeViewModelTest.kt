package dev.marcosfarias.pokedex

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dev.marcosfarias.pokedex.model.Menu
import dev.marcosfarias.pokedex.ui.home.HomeViewModel
import io.mockk.unmockkAll
import io.qameta.allure.Description
import org.junit.*

class HomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun before() {
        viewModel = HomeViewModel()
    }

    @Test
    @Description("Проверяет получение списка меню")
    fun `GIVEN list of menus WHEN call function THEN result as expected`() {
        // GIVEN
        val expected = listOf(
            Menu(id = 1, name = R.string.menu_item_1, color = R.color.lightTeal),
            Menu(id = 1, name = R.string.menu_item_2, color = R.color.lightRed),
            Menu(id = 1, name = R.string.menu_item_3, color = R.color.lightBlue),
            Menu(id = 1, name = R.string.menu_item_4, color = R.color.lightYellow),
            Menu(id = 1, name = R.string.menu_item_5, color = R.color.lightPurple),
            Menu(id = 1, name = R.string.menu_item_6, color = R.color.lightBrown)
        )

        // WHEN
        val result = viewModel.getListMenu()

        // THEN
        val expectedTransformed = expected.map { it.toString() }
        val resultTransformed = result.value!!.map { it.toString() }
        Assert.assertEquals(expectedTransformed, resultTransformed)
    }

    @Test
    @Description("Проверяет получение списка новостей")
    fun `GIVEN list of news WHEN call function THEN result expected size`() {
        // GIVEN
        val expectedSize = 8

        // WHEN
        val result = viewModel.getListNews()

        // THEN
        Assert.assertEquals(expectedSize, result.value!!.size)
    }

    @Test
    @Description("Проверяет корректность структуры данных меню")
    fun `GIVEN HomeViewModel WHEN get menu THEN menu items have correct structure`() {
        // Given
        val menuList = viewModel.getListMenu().value!!

        // Then
        menuList.forEach { menu ->
            Assert.assertNotNull("Menu ID should not be null", menu.id)
            Assert.assertNotNull("Menu name should not be null", menu.name)
            Assert.assertNotNull("Menu color should not be null", menu.color)
        }

        // Проверяем конкретные цвета меню
        val pokedexMenu = menuList.find { it.name == R.string.menu_item_1 }
        Assert.assertEquals("Pokedex menu should have lightTeal color", R.color.lightTeal, pokedexMenu?.color)
    }

    @Test
    @Description("Проверяет наличие ожидаемых элементов в меню")
    fun `GIVEN HomeViewModel WHEN get menu list THEN return correct menu items`() {
        // When
        val result = viewModel.getListMenu()

        // Then
        Assert.assertNotNull("Menu list should not be null", result.value)
        Assert.assertTrue("Menu list should not be empty", result.value!!.isNotEmpty())

        // Проверяем что меню содержит ожидаемые элементы
        val menuTitles = result.value!!.map { it.name }
        Assert.assertTrue("Should contain Pokedex menu item", menuTitles.contains(R.string.menu_item_1))
        Assert.assertTrue("Should contain Pokemon menu item", menuTitles.contains(R.string.menu_item_2))
    }

    @Test
    @Description("Проверяет что новости создаются корректно (пустой класс)")
    fun `GIVEN HomeViewModel WHEN get news list THEN return list of News objects`() {
        // When
        val result = viewModel.getListNews()

        // Then
        Assert.assertNotNull("News list should not be null", result.value)
        Assert.assertTrue("News list should not be empty", result.value!!.isNotEmpty())

        // Проверяем что все элементы являются экземплярами News
        result.value!!.forEach { news ->
            Assert.assertTrue("Each item should be instance of News", news is dev.marcosfarias.pokedex.model.News)
        }
    }

    @Test
    @Description("Проверяет что список новостей инициализируется корректно")
    fun `GIVEN HomeViewModel WHEN initialized THEN news list is properly initialized`() {
        // When
        val newsList = viewModel.getListNews()

        // Then
        Assert.assertNotNull("News LiveData should not be null", newsList)
        Assert.assertNotNull("News list value should not be null", newsList.value)
        Assert.assertEquals("Should return 8 news items", 8, newsList.value!!.size)

        // Все элементы должны быть экземплярами News (даже если класс пустой)
        newsList.value!!.forEach { news ->
            Assert.assertEquals("Should be News class", "dev.marcosfarias.pokedex.model.News", news::class.qualifiedName)
        }
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun tearDown() {
            unmockkAll()
        }
    }
}