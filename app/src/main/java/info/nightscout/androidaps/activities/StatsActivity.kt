package info.nightscout.androidaps.activities

import android.os.Bundle
import info.nightscout.androidaps.R
import info.nightscout.androidaps.utils.ActivityMonitor
import info.nightscout.androidaps.utils.OKDialog
import info.nightscout.androidaps.utils.TddCalculator
import info.nightscout.androidaps.utils.TirCalculator
import info.nightscout.androidaps.utils.resources.ResourceHelper
import kotlinx.android.synthetic.main.stats_activity.*
import javax.inject.Inject

class StatsActivity : NoSplashAppCompatActivity() {
    @Inject lateinit var tddCalculator: TddCalculator
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var activityMonitor: ActivityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats_activity)

        stats_tdds.text = tddCalculator.stats()
        stats_tir.text = TirCalculator.stats()
        stats_activity.text = activityMonitor.stats()

        ok.setOnClickListener { finish() }
        stats_reset.setOnClickListener {
            OKDialog.showConfirmation(this, resourceHelper.gs(R.string.doyouwantresetstats), Runnable {
                activityMonitor.reset()
                recreate()
            })
        }
    }
}
