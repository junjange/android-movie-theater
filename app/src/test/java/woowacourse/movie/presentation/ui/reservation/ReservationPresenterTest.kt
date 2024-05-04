package woowacourse.movie.presentation.ui.reservation

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import woowacourse.movie.domain.repository.ReservationRepository
import woowacourse.movie.domain.repository.ScreenRepository
import woowacourse.movie.presentation.ui.utils.DummyData.dummyReservation

@ExtendWith(MockKExtension::class)
class ReservationPresenterTest {
    @MockK
    private lateinit var view: ReservationContract.View

    private lateinit var presenter: ReservationContract.Presenter

    @MockK
    private lateinit var repository: ReservationRepository

    @MockK
    private lateinit var theaterRepository: ScreenRepository

    @BeforeEach
    fun setUp() {
        presenter = ReservationPresenter(view, repository, theaterRepository)
    }

    @Test
    fun `ReservationPresenter가 유효한 예매 id를 통해 loadReservation()을 했을 때, view에게 reservation 데이터를 전달한다`() {
        // given
        every { theaterRepository.findTheaterNameById(any()) } returns Result.success("선릉")
        every { repository.findByReservationId(any()) } returns Result.success(dummyReservation)
        every { view.showReservation(any(), any()) } just runs

        // when
        presenter.loadReservation(1)

        // then
        verify { view.showReservation(any(), any()) }
    }

    @Test
    fun `ScreenPresenter가 유효하지 않은 예매 id를 통해 loadReservation()했을 때, view에게 back과 throwable를 전달한다`() {
        // given
        every { repository.findByReservationId(any()) } returns
            Result.failure(
                NoSuchElementException(),
            )
        every { view.showToastMessage(e = any()) } just runs
        every { view.navigateBackToPrevious() } just runs

        // when
        presenter.loadReservation(1)

        // then
        verify { view.showToastMessage(e = any()) }
        verify { view.navigateBackToPrevious() }
    }

    @Test
    fun `ScreenPresenter가 loadReservation()했을 때, 예상치 못한 에러가 발생하면 view에게 back과 throwable를 전달한다`() {
        // given
        every { repository.findByReservationId(any()) } returns Result.failure(Exception())
        every { view.showToastMessage(e = any()) } just runs
        every { view.navigateBackToPrevious() } just runs

        // when
        presenter.loadReservation(1)

        // then
        verify { view.showToastMessage(e = any()) }
        verify { view.navigateBackToPrevious() }
    }
}
