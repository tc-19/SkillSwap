package com.tubacelik.myapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.tubacelik.myapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    //Verbindung zwischen Java und activity_main.xml
    private ActivityMainBinding binding;

    //steuert Navigation zwischen Fragments
    private NavController navController;

    //legt fest, welche Fragmente als Hauptseiten gelten
    private AppBarConfiguration appBarConfiguration;

    //wird beim start der app einmal ausgeführt
    // Oberfläche wird aufgebaut und navigation eingerichtet
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar); //verwendet toolbar als obere app-leiste

        //sucht NavHostFragment aus activity_main.xml
        //dort werden alle Fragments angezeigt
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(
                                R.id.nav_host_fragment_content_main
                        );

        //Sicherheitsprüfung
        //falls kein NavHostFragment gefunden wurde, wird Methode beendet
        if (navHostFragment == null) {
            return;
        }

        //holt den NavController,
        //der Navigation zwischen Fragments steuert
        navController = navHostFragment.getNavController();

        //hauptseiten der App
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.SecondFragment,
                R.id.BrowseSkillsFragment,
                R.id.AddSkillFragment,
                R.id.RequestsFragment,
                R.id.MySkillsFragment
        ).build();

        //verbindet toolbar mit NavController
        //damit Titel automatisch funktioniert
        NavigationUI.setupActionBarWithNavController(
                this,
                navController,
                appBarConfiguration
        );

        NavigationUI.setupWithNavController(
                binding.bottomNavigation,
                navController
        );

        //Listener reagiert jedes mal, wenn zu einem anderen Fragment navigiert wird
        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {
                    boolean loginScreen =
                            destination.getId() == R.id.FirstFragment;

                    //blendet Bottom Navigation im Login-Screen aus
                    // sonst wird angezeigt
                    binding.bottomNavigation.setVisibility(
                            loginScreen ? View.GONE : View.VISIBLE
                    );

                    binding.appBarLayout.setVisibility(
                            loginScreen ? View.GONE : View.VISIBLE
                    );
                }
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp( //navigiert eine Seite zurück mithilfe NavController
                navController,
                appBarConfiguration
        ) || super.onSupportNavigateUp();
    }
}