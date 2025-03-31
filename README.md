# Project Mosquito

Project Mosquito is an Android application designed to connect to DJI drones, process their video feed to detect and read QR codes and barcodes, and interact with an ERP system for warehouse cyclic counting. The app automates inventory management tasks by leveraging drone technology and barcode scanning.

## Features

- Connects to DJI drones for live video feed.
- Detects and reads QR codes and barcodes from the drone's camera feed.
- Integrates with an ERP system to perform:
  - Warehouse cyclic counting.
  - Inventory updates based on scanned data.
- Supports multiple barcode formats.

## Demo

Watch a live demo of the application in action: [Project Mosquito Demo](https://www.youtube.com/live/YRACUrHMp40?si=-yJ8XwZWFhdIaFIr&t=5489)

## Requirements

- Android Studio 4.1 or later.
- Gradle 6.5 or later.
- DJI SDK version 4.14.1.
- Android device with API level 28 or higher.
- DJI drone compatible with the DJI SDK.

## Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-repo/project-mosquito.git
   cd project-mosquito
   ```

2. **Install Dependencies**:
   Open the project in Android Studio. Gradle will automatically download the required dependencies.

3. **Configure DJI SDK**:
   - Obtain a DJI Developer account and register your application.
   - Add your DJI App Key to the `AndroidManifest.xml` file:
     ```xml
     <meta-data
         android:name="com.dji.sdk.API_KEY"
         android:value="YOUR_DJI_APP_KEY" />
     ```

4. **Build and Run**:
   - Connect your Android device.
   - Select the desired build variant (e.g., `debug`).
   - Click on the "Run" button in Android Studio.

## Usage

1. Launch the app on your Android device.
2. Connect to a DJI drone using the app's interface.
3. Start the live video feed from the drone.
4. Point the drone's camera at QR codes or barcodes.
5. The app will automatically scan the codes and send the data to the ERP system for processing.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Commit your changes and push the branch.
4. Open a pull request describing your changes.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [DJI SDK](https://developer.dji.com/) for enabling drone integration.
- Open-source libraries used in this project.

