package info.nightscout.androidaps.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import info.nightscout.androidaps.R
import info.nightscout.androidaps.data.defaultProfile.DefaultProfile
import info.nightscout.androidaps.dialogs.ProfileViewerDialog
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunction
import info.nightscout.androidaps.utils.*
import info.nightscout.androidaps.utils.resources.ResourceHelper
import kotlinx.android.synthetic.main.survey_activity.*
import javax.inject.Inject

class SurveyActivity : NoSplashAppCompatActivity() {
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var configBuilderPlugin: ConfigBuilderPlugin
    @Inject lateinit var tddCalculator: TddCalculator
    @Inject lateinit var profileFunction: ProfileFunction
    @Inject lateinit var activityMonitor: ActivityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.survey_activity)

        survey_id.text = InstanceId.instanceId()

        val profileStore = configBuilderPlugin.activeProfileInterface.profile
        val profileList = profileStore?.getProfileList() ?: return
        survey_spinner.adapter = ArrayAdapter(this, R.layout.spinner_centered, profileList)

        survey_tdds.text = tddCalculator.stats()
        survey_tir.text = TirCalculator.stats()
        survey_activity.text = activityMonitor.stats()

        survey_profile.setOnClickListener {
            val age = SafeParse.stringToDouble(survey_age.text.toString())
            val weight = SafeParse.stringToDouble(survey_weight.text.toString())
            val tdd = SafeParse.stringToDouble(survey_tdd.text.toString())
            if (age < 1 || age > 120) {
                ToastUtils.showToastInUiThread(this, R.string.invalidage)
                return@setOnClickListener
            }
            if ((weight < 5 || weight > 150) && tdd == 0.0) {
                ToastUtils.showToastInUiThread(this, R.string.invalidweight)
                return@setOnClickListener
            }
            if ((tdd < 5 || tdd > 150) && weight == 0.0) {
                ToastUtils.showToastInUiThread(this, R.string.invalidweight)
                return@setOnClickListener
            }
            val profile = DefaultProfile().profile(age, tdd, weight, profileFunction.getUnits())
            val args = Bundle()
            args.putLong("time", DateUtil.now())
            args.putInt("mode", ProfileViewerDialog.Mode.CUSTOM_PROFILE.ordinal)
            args.putString("customProfile", profile.data.toString())
            args.putString("customProfileUnits", profile.units)
            args.putString("customProfileName", "Age: $age TDD: $tdd Weight: $weight")
            val pvd = ProfileViewerDialog()
            pvd.arguments = args
            pvd.show(supportFragmentManager, "ProfileViewDialog")
        }

        survey_submit.setOnClickListener {
            val r = FirebaseRecord()
            r.id = InstanceId.instanceId()
            r.age = SafeParse.stringToInt(survey_age.text.toString())
            r.weight = SafeParse.stringToInt(survey_weight.text.toString())
            if (r.age < 1 || r.age > 120) {
                ToastUtils.showToastInUiThread(this, R.string.invalidage)
                return@setOnClickListener
            }
            if (r.weight < 5 || r.weight > 150) {
                ToastUtils.showToastInUiThread(this, R.string.invalidweight)
                return@setOnClickListener
            }

            if (survey_spinner.selectedItem == null)
                return@setOnClickListener
            val profileName = survey_spinner.selectedItem.toString()
            val specificProfile = profileStore.getSpecificProfile(profileName)

            r.profileJson = specificProfile.toString()

            val auth = FirebaseAuth.getInstance()
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        aapsLogger.debug(LTag.CORE, "signInAnonymously:success")
                        //val user = auth.currentUser // TODO: do we need this, seems unused?

                        val database = FirebaseDatabase.getInstance().reference
                        database.child("survey").child(r.id).setValue(r)
                    } else {
                        aapsLogger.error("signInAnonymously:failure", task.exception!!)
                        ToastUtils.showToastInUiThread(this, "Authentication failed.")
                        //updateUI(null)
                    }

                    // ...
                }
            finish()
        }
    }

    inner class FirebaseRecord {
        var id = ""
        var age: Int = 0
        var weight: Int = 0
        var profileJson = "ghfg"
    }

}
