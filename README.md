# PiTV Explorer

## Overview
PiTV Explorer is an Android TV application that allows you to browse and access media files stored on a Raspberry Pi server. This app provides a TV-friendly interface for navigating directories, viewing images, and playing videos directly on your Android TV.

## Features
- Simple server connection with IP address input
- File browsing with support for directories and various file types
- Image gallery viewer with full-screen mode
- Video player with playback controls
- TV remote-friendly navigation
- Grid view for easy browsing on large screens
- Automatic caching of server IP for future sessions


## Requirements
- Android TV device (Android 5.0+)
- Running FilePi server instance (available at [PiView](https://github.com/renjuashokan/pi_view](https://github.com/renjuashokan/pi_view))
- Network connectivity between your Android TV and Raspberry Pi

## Installation
1. Download the latest APK from the Releases section
2. Install on your Android TV using a file explorer or adb
3. Launch the app from your Android TV home screen

## Usage
### First-time setup
1. Launch PiTV Explorer from your Android TV
2. Enter your Raspberry Pi server's IP address (e.g., 192.168.1.100)
3. Press the Connect button

### Browsing files
- Use D-pad to navigate between files and folders
- Press Select to open folders or view files
- Press Back to navigate up one directory level
- Use on-screen controls to sort files or toggle between grid and list views

### Image viewing
- Navigate between images using left/right D-pad
- Toggle grid view to see image thumbnails
- Full-screen mode for optimal viewing

### Video playback
- Basic playback controls (play, pause, skip)
- Video information display
- Support for common video formats

## Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/PiTVExplorer.git
   ```

2. Open the project in Android Studio

3. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

## Server Requirements
The app is designed to work with the FilePi server, which provides the following API endpoints:
- `/api/v1/files` - List files in a directory
- `/api/v1/file/[path]` - Get file content
- `/api/v1/thumbnail/[path]` - Get video thumbnail
- `/api/v1/stream/[path]` - Stream video file
- `/api/v1/search` - Search for files

Ensure your server is running and accessible on port 8080.

## Troubleshooting
- **Connection issues**: Verify your Raspberry Pi server is running and accessible on the network
- **File browsing problems**: Check server permissions and ensure API endpoints are correctly configured
- **Playback issues**: Verify the media format is supported by Android TV

## Future Development
- Add search functionality
- Implement custom sorting options
- Add support for audio files
- Improve UI/UX for TV remote navigation
- Add settings screen for customization

## Related Projects
- [Pi View](https://github.com/renjuashokan/pi_view) - Flutter mobile client for the same server

## License
MIT License

## Acknowledgements
- [Android TV Leanback Library](https://developer.android.com/training/tv/start/start)
- [ExoPlayer](https://exoplayer.dev/) for video playback capabilities
- [Glide](https://github.com/bumptech/glide) for image loading and caching

---
