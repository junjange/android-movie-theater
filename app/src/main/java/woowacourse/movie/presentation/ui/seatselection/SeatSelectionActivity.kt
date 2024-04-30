package woowacourse.movie.presentation.ui.seatselection

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import woowacourse.movie.R
import woowacourse.movie.domain.model.ScreenView
import woowacourse.movie.domain.model.Seat
import woowacourse.movie.domain.model.SeatRank
import woowacourse.movie.domain.repository.DummyReservation
import woowacourse.movie.domain.repository.DummyScreens
import woowacourse.movie.presentation.base.BaseActivity
import woowacourse.movie.presentation.model.ReservationInfo
import woowacourse.movie.presentation.model.UserSeat
import woowacourse.movie.presentation.ui.reservation.ReservationActivity
import woowacourse.movie.presentation.ui.seatselection.SeatSelectionContract.View
import woowacourse.movie.presentation.utils.currency
import java.io.Serializable

class SeatSelectionActivity : BaseActivity(), View {
    override val layoutResourceId: Int
        get() = R.layout.activity_seat_selection
    override val presenter: SeatSelectionPresenter by lazy {
        SeatSelectionPresenter(this, DummyScreens(), DummyReservation)
    }

    private val seatBoard: TableLayout by lazy { findViewById(R.id.tl_seat_board) }
    private val title: TextView by lazy { findViewById(R.id.tv_screen_title) }
    private val totalPrice: TextView by lazy { findViewById(R.id.tv_screen_total_price) }
    private val btnDone: Button by lazy { findViewById(R.id.btn_done) }

    override fun initStartView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val reservationInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    PUT_EXTRA_KEY_RESERVATION_INFO,
                    ReservationInfo::class.java,
                )
            } else {
                intent.getSerializableExtra(PUT_EXTRA_KEY_RESERVATION_INFO) as ReservationInfo
            }

        reservationInfo?.let { reservationInfoItem ->
            presenter.updateUiModel(reservationInfoItem)
            presenter.loadScreen(reservationInfoItem.screenId)
            presenter.loadSeatBoard(reservationInfoItem.screenId)
        }
    }

    override fun showScreen(movie: ScreenView.Movie) {
        with(movie) {
            this@SeatSelectionActivity.title.text = movie.title
            totalPrice.text = DEFAULT_TOTAL_PRICE.currency(this@SeatSelectionActivity)
            btnDone.isEnabled = false
        }
    }

    override fun showSeatBoard(seats: List<Seat>) {
        seatBoard.children.filterIsInstance<TableRow>().flatMap { it.children }
            .filterIsInstance<TextView>().forEachIndexed { idx, view ->
                view.text = "${seats[idx].column}${seats[idx].row + 1}"
                view.setTextColor(seats[idx].seatRank.toColor())
            }
    }

    override fun initClickListener(seats: List<Seat>) {
        seatBoard.children.filterIsInstance<TableRow>().flatMap { it.children }
            .filterIsInstance<TextView>().forEachIndexed { idx, view ->
                view.setOnClickListener {
                    presenter.clickSeat(seats[idx])
                    presenter.calculateSeat()
                    presenter.checkAllSeatsSelected()
                }
            }

        btnDone.setOnClickListener {
            showReservationDialog()
        }
    }

    private fun SeatRank.toColor(): Int {
        return when (this) {
            SeatRank.B -> getColor(R.color.purple)
            SeatRank.S -> getColor(R.color.green)
            SeatRank.A -> getColor(R.color.blue)
        }
    }

    override fun selectSeat(
        column: Int,
        row: Int,
    ) {
        seatBoard.children.filterIsInstance<TableRow>().flatMap { it.children }
            .filterIsInstance<TextView>().toList()[column * 4 + row].setBackgroundColor(
            ContextCompat.getColor(this, R.color.yellow),
        )
    }

    override fun unselectSeat(
        column: Int,
        row: Int,
    ) {
        seatBoard.children.filterIsInstance<TableRow>().flatMap { it.children }
            .filterIsInstance<TextView>().toList()[column * 4 + row].setBackgroundColor(
            ContextCompat.getColor(this, R.color.white),
        )
    }

    override fun showTotalPrice(totalPrice: Int) {
        this.totalPrice.text = totalPrice.currency(this)
    }

    override fun buttonEnabled(isActivate: Boolean) {
        btnDone.isEnabled = isActivate
    }

    private fun showReservationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_reservation_title))
        builder.setMessage(getString(R.string.dialog_reservation_message))
        builder.setCancelable(false)

        builder.setPositiveButton(getString(R.string.reservation_done)) { _, _ ->
            presenter.reserve()
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun navigateToReservation(id: Int) {
        ReservationActivity.startActivity(this, id)
        back()
    }

    override fun back() = finish()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        back()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PUT_STATE_KEY_USER_SEAT, presenter.uiModel.userSeat)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val savedUserSeat = savedInstanceState.getSerializable(PUT_STATE_KEY_USER_SEAT) as UserSeat?
        savedUserSeat?.let { userSeat ->
            userSeat.seats.forEach { seat ->
                presenter.clickSeat(seat)
            }
            presenter.calculateSeat()
            presenter.checkAllSeatsSelected()
        }
    }

    companion object {
        private const val DEFAULT_TOTAL_PRICE = 0

        private const val PUT_EXTRA_KEY_RESERVATION_INFO = "reservationInfo"
        private const val PUT_STATE_KEY_USER_SEAT = "userSeat"

        fun startActivity(
            context: Context,
            reservationInfo: ReservationInfo,
        ) {
            val intent = Intent(context, SeatSelectionActivity::class.java)
            intent.putExtra(PUT_EXTRA_KEY_RESERVATION_INFO, reservationInfo as Serializable)
            context.startActivity(intent)
        }
    }
}