# app-migratory-platform-android

## Prerequisites
- Android SDK/NDK
- npm

## Usage
Clone the repository
``` bash
$ git clone https://github.com/teamellipsis/app-migratory-platform-android
```

``` bash
# Change the working directory to project directory
cd app-migratory-platform-android

# Then change the working directory to assets node directory
cd app/src/main/assets/node

# Install npm modules
npm install --production
```

Create ZIP file with the name `node_modules.zip` in `./app/src/main/assets/node`.
```
node_modules.zip
    ⊢ node_modules
        ⊢ ...
        ⊢ react
        ⊢ redux
        ⊢ next
        ⊢ ...
```

Open using Android Studio
