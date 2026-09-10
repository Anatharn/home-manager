// Empêche l'ouverture d'une console Windows en plus de la fenêtre de l'app.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod spring_launcher;

use spring_launcher::SpringProcess;
use tauri::{Manager, WindowEvent};

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(SpringProcess::new())
        .setup(|app| {
            let handle = app.handle().clone();
            spring_launcher::start_spring(&handle);
            Ok(())
        })
        .on_window_event(|window, event| {
            // Coupe le backend Spring quand la fenêtre principale se ferme,
            // pour ne pas laisser un jar orphelin tourner en arrière-plan.
            if let WindowEvent::CloseRequested { .. } = event {
                spring_launcher::stop_spring(window.app_handle());
            }
        })
        .run(tauri::generate_context!())
        .expect("erreur au démarrage de l'application Tauri");
}
