use std::path::PathBuf;
use std::process::{Child, Command};
use std::sync::Mutex;

use tauri::{AppHandle, Manager};

/// Garde une référence au process Spring pour pouvoir le stopper proprement
/// à la fermeture de l'application.
pub struct SpringProcess(pub Mutex<Option<Child>>);

impl SpringProcess {
    pub fn new() -> Self {
        SpringProcess(Mutex::new(None))
    }
}

/// Démarre le backend Spring (jar embarqué dans les ressources de l'app)
/// en process enfant, et enregistre son handle dans l'état de l'app.
pub fn start_spring(app: &AppHandle) {
    let jar_path = resolve_jar_path(app);

    if !jar_path.exists() {
        eprintln!("[spring_launcher] jar introuvable à l'emplacement : {:?}", jar_path);
        return;
    }

    println!("[spring_launcher] démarrage du backend Spring depuis {:?}", jar_path);

    let mut command = Command::new("java");
    command.arg("-jar").arg(&jar_path);

    // En debug (tauri dev), on laisse le backend sur son profil par défaut ("dev", H2 —
    // voir application.yml). En build release, on force "prod" : la connexion doit venir de
    // la configuration utilisateur (vue Configuration > Base de données), pas d'une base H2
    // embarquée — voir application-prod.yml.
    if !cfg!(debug_assertions) {
        command.env("SPRING_PROFILES_ACTIVE", "prod");
    }

    match command.spawn() {
        Ok(child) => {
            let state = app.state::<SpringProcess>();
            *state.0.lock().unwrap() = Some(child);
        }
        Err(err) => {
            eprintln!("[spring_launcher] échec du démarrage du backend Spring : {err}");
        }
    }
}

/// Arrête le process Spring s'il est encore actif. À appeler à la fermeture
/// de la fenêtre principale pour éviter de laisser un jar orphelin tourner.
pub fn stop_spring(app: &AppHandle) {
    let state = app.state::<SpringProcess>();
    let child = state.0.lock().unwrap().take();
    if let Some(mut child) = child {
        println!("[spring_launcher] arrêt du backend Spring");
        let _ = child.kill();
    }
}

/// En dev, le jar est cherché dans src-tauri/resources/backend.jar (copié
/// manuellement ou via un script avant `tauri dev`).
/// En prod, Tauri place les ressources déclarées dans tauri.conf.json
/// dans le resource_dir() de l'app installée.
fn resolve_jar_path(app: &AppHandle) -> PathBuf {
    if cfg!(debug_assertions) {
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("resources")
            .join("backend.jar")
    } else {
        app.path()
            .resource_dir()
            .expect("resource_dir introuvable")
            .join("resources")
            .join("backend.jar")
    }
}
