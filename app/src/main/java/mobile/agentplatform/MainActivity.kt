package mobile.agentplatform

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.Window


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var appConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        appToolbar.title = resources.getString(R.string.title_app_management_fragment)
        setSupportActionBar(appToolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, appToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.menu.getItem(0).isChecked = true
        val fragment = AppManagementFragment()
        supportFragmentManager.beginTransaction()
            .replace(content_frame.id, fragment)
            .commit()

        appConfig = AppConfig(applicationContext)

        if (appConfig.get(AppConstant.KEY_WORKING_DIR).isEmpty()) {
            val intent = Intent(applicationContext, FreshConfigActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        lateinit var fragment: Fragment
        when (item.itemId) {
            R.id.nav_apps -> {
                appToolbar.title = resources.getString(R.string.title_app_management_fragment)
                fragment = AppManagementFragment()
            }
            R.id.nav_devices -> {
                appToolbar.title = resources.getString(R.string.title_devices_fragment)
                fragment = DevicesFragment()
            }
            R.id.nav_share -> {
                appToolbar.title = resources.getString(R.string.title_sharing_fragment)
                fragment = SharingFragment()
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(content_frame.id, fragment)
            .commit()

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val fragment = supportFragmentManager.findFragmentById(R.id.content_frame) as DrawerFragmentInterface
        fragment.onNewIntent(intent)
    }
}
