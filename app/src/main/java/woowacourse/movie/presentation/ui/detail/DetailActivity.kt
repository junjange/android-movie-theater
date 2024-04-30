package woowacourse.movie.presentation.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import woowacourse.movie.R
import woowacourse.movie.domain.model.Screen
import woowacourse.movie.domain.model.ScreenDate
import woowacourse.movie.domain.repository.DummyScreens
import woowacourse.movie.presentation.base.BaseActivity
import woowacourse.movie.presentation.model.ReservationInfo
import woowacourse.movie.presentation.ui.detail.DetailContract.View
import woowacourse.movie.presentation.ui.seatselection.SeatSelectionActivity
import java.time.LocalDate
import java.time.LocalTime

class DetailActivity : BaseActivity(), View {
    override val layoutResourceId: Int
        get() = R.layout.activity_detail
    override val presenter: DetailPresenter by lazy { DetailPresenter(this, DummyScreens()) }

    private val title: TextView by lazy { findViewById(R.id.tv_title) }
    private val date: TextView by lazy { findViewById(R.id.tv_screen_date) }
    private val runningTime: TextView by lazy { findViewById(R.id.tv_screen_running_time) }
    private val description: TextView by lazy { findViewById(R.id.tv_description) }
    private val poster: ImageView by lazy { findViewById(R.id.iv_poster) }
    private val ticketCount: TextView by lazy { findViewById(R.id.tv_count) }
    private val plusBtn: Button by lazy { findViewById(R.id.btn_plus) }
    private val minusBtn: Button by lazy { findViewById(R.id.btn_minus) }
    private val selectSeatBtn: Button by lazy { findViewById(R.id.btn_select_seat) }
    private val dateSpinner: Spinner by lazy { findViewById(R.id.spn_date) }
    private val timeSpinner: Spinner by lazy { findViewById(R.id.spn_time) }

    override fun initStartView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val movieId = intent.getIntExtra(PUT_EXTRA_KEY_MOVIE_ID, DEFAULT_ID)
        val theaterId = intent.getIntExtra(PUT_EXTRA_KEY_THEATER_ID, DEFAULT_ID)

        presenter.loadScreen(movieId, theaterId)
        initClickListener()
        initItemSelectedListener()
    }

    private fun initClickListener() {
        plusBtn.setOnClickListener {
            presenter.plusTicket()
        }

        minusBtn.setOnClickListener {
            presenter.minusTicket()
        }

        selectSeatBtn.setOnClickListener {
            presenter.selectSeat()
        }
    }

    private fun initItemSelectedListener() {
        dateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    val localDate = parent.getItemAtPosition(position) as LocalDate
                    if (presenter.uiModel.selectedDate?.date != localDate) {
                        presenter.registerDate(localDate)
                        presenter.createTimeSpinnerAdapter(ScreenDate(localDate))
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        timeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    presenter.registerTime(parent.getItemAtPosition(position) as LocalTime)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    override fun showScreen(screen: Screen) {
        with(screen) {
            title.text = movie.title
            this@DetailActivity.date.text = getString(R.string.screening_period, movie.startDate.toString(), movie.endDate.toString())
            runningTime.text = movie.runningTime.toString()
            description.text = movie.description
            poster.setImageResource(movie.imageSrc)
            presenter.createDateSpinnerAdapter(selectableDates)
            presenter.createTimeSpinnerAdapter(selectableDates.first())
        }
    }

    override fun showDateSpinnerAdapter(screenDates: List<ScreenDate>) {
        dateSpinner.adapter =
            ArrayAdapter(
                this@DetailActivity,
                android.R.layout.simple_spinner_item,
                screenDates.map { it.date },
            )
    }

    override fun showTimeSpinnerAdapter(screenDate: ScreenDate) {
        timeSpinner.adapter =
            ArrayAdapter(
                this@DetailActivity,
                android.R.layout.simple_spinner_item,
                screenDate.getSelectableTimes().map { it },
            )
    }

    override fun showTicket(count: Int) {
        ticketCount.text = count.toString()
    }

    override fun navigateToSeatSelection(reservationInfo: ReservationInfo) {
        SeatSelectionActivity.startActivity(this, reservationInfo)
        back()
    }

    override fun back() = finish()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        back()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PUT_TICKET_STATE_KEY, presenter.uiModel.ticket.count)
        outState.putSerializable(PUT_STATE_KEY_SELECTED_DATE, presenter.uiModel.selectedDate?.date)
        outState.putSerializable(PUT_STATE_KEY_SELECTED_TIME, presenter.uiModel.selectedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        restoreTicketCount(savedInstanceState)
        restoreSelectedDate(savedInstanceState)
        restoreSelectedTime(savedInstanceState)
    }

    private fun restoreTicketCount(savedInstanceState: Bundle) {
        val count = savedInstanceState.getInt(PUT_TICKET_STATE_KEY, DEFAULT_TICKET_COUNT)
        if (count != DEFAULT_TICKET_COUNT) {
            presenter.updateTicket(count)
        }
    }

    private fun restoreSelectedDate(savedInstanceState: Bundle) {
        val savedLocalDate =
            savedInstanceState.getSerializable(PUT_STATE_KEY_SELECTED_DATE) as LocalDate?
        savedLocalDate?.let { localDate ->
            presenter.registerDate(localDate)
            val position = findPositionForSelectedDate(localDate)
            presenter.createTimeSpinnerAdapter(ScreenDate(localDate))
            dateSpinner.setSelection(position)
        }
    }

    private fun findPositionForSelectedDate(selectedDate: LocalDate): Int {
        presenter.uiModel.selectableDates.forEachIndexed { index, screenDate ->
            if (screenDate.date == selectedDate) {
                return index
            }
        }
        return 0
    }

    private fun restoreSelectedTime(savedInstanceState: Bundle) {
        val savedLocalTime =
            savedInstanceState.getSerializable(PUT_STATE_KEY_SELECTED_TIME) as LocalTime?
        savedLocalTime?.let { localTime ->
            presenter.registerTime(localTime)
            val position = findPositionForSelectedTime(localTime)
            timeSpinner.setSelection(position)
        }
    }

    private fun findPositionForSelectedTime(selectedTime: LocalTime): Int {
        presenter.uiModel.selectedDate?.let { screenDate ->
            screenDate.getSelectableTimes().forEachIndexed { index, screenTime ->
                if (screenTime == selectedTime) {
                    return index
                }
            }
        }
        return 0
    }

    companion object {
        private const val DEFAULT_ID = -1
        private const val PUT_EXTRA_KEY_MOVIE_ID = "movieId"
        private const val PUT_EXTRA_KEY_THEATER_ID = "theaterId"

        private const val DEFAULT_TICKET_COUNT = -1
        private const val PUT_TICKET_STATE_KEY = "ticketCount"
        private const val PUT_STATE_KEY_SELECTED_DATE = "selectedDate"
        private const val PUT_STATE_KEY_SELECTED_TIME = "selectedTime"

        fun startActivity(
            context: Context,
            movieId: Int,
            theaterId: Int,
        ) {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(PUT_EXTRA_KEY_MOVIE_ID, movieId)
            intent.putExtra(PUT_EXTRA_KEY_THEATER_ID, theaterId)
            context.startActivity(intent)
        }
    }
}