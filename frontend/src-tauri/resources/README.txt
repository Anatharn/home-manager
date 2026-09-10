Ce dossier doit contenir "backend.jar" avant de lancer `npm run tauri dev` ou de builder l'app.

Génère ce jar depuis le dossier backend/ :

    cd ../../backend
    mvn clean package
    cp target/home-butler.jar ../frontend/src-tauri/resources/backend.jar

Le script scripts/build-all.sh (à la racine du projet) automatise cette copie.
