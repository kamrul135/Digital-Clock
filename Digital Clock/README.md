# Digital Clock Project

This project showcases various implementations of digital and analog clocks using Java Swing, with a focus on demonstrating threading concepts and GUI development.

## Project Structure

The project consists of several clock implementations with increasing complexity:

- **DigitalClock**: Basic digital clock implementation
- **AnalogClock**: A graphical analog clock
- **WorldTimeZones**: A clock that displays time across different time zones
- **AdvancedDigitalClock**: Comprehensive implementation combining all features

### Directory Structure

```
Digital Clock/
├── README.md             # Project documentation
├── build/                # Compiled class files
└── Src/                  # Source code directory
    ├── DigitalClock.java       # Basic digital clock implementation
    ├── AnalogClock.java        # Analog clock implementation
    ├── WorldTimeZones.java     # World time zones implementation
    └── AdvancedDigitalClock.java  # Advanced implementation with all features
```

### Implementation Hierarchy

The implementations build upon each other in complexity:

1. **DigitalClock.java** - Basic implementation with:
   - Simple time display
   - Basic thread management
   - Minimal UI

2. **AnalogClock.java** - Adds:
   - Graphical clock face
   - Hour, minute, and second hands
   - Advanced graphics rendering

3. **WorldTimeZones.java** - Adds:
   - Multiple time zone support
   - Zone selection interface
   - Date and time formatting

4. **AdvancedDigitalClock.java** - Combines all features plus:
   - Thread pool management
   - Luxury watch styling
   - Enhanced UI components
   - Millisecond precision

## Advanced Digital Clock Features

The `AdvancedDigitalClock` is the most sophisticated implementation, featuring:

- Digital time display with milliseconds
- Elegant analog clock with luxury watch styling
- Multiple concurrent threads managed through ExecutorService
- Thread pool management and interruption handling
- World time display for major cities across different time zones
- Start, pause, stop, and exit functionality
- Graceful thread shutdown

## Threading Concepts Demonstrated

This project demonstrates several threading concepts:

1. **ExecutorService**: Used for thread pool management
2. **Future objects**: For thread result handling
3. **Multiple concurrent threads**: Running different clock components
4. **Thread priority management**: Handling different update frequencies
5. **Thread interruption handling**: Clean shutdown of threads
6. **SwingUtilities.invokeLater()**: For thread-safe GUI updates

## Analog Clock Design

The analog clock features a luxury watch design inspired by Rolex, including:

- Gold bezel with fluted edge design
- Sunburst dial face
- Mercedes-style hour hand
- Classic minute and second hands
- Roman numeral hour markers
- Detailed center crown

## Running the Application

To run the advanced digital clock:

```
java -cp ./Src AdvancedDigitalClock
```

## Technical Implementation

### Class Structure

- **JFrame Extensions**: All clock implementations extend `JFrame` for window management
- **Custom JPanels**: Used for specialized drawing areas (especially in analog clock)
- **Runnable Implementations**: For threading functionality
- **Event Listeners**: For button controls and UI interaction

### Key Java Concepts Used

- **Swing/AWT**: For GUI components and rendering
- **Concurrency Utilities**: `ExecutorService`, `Future`, thread management
- **Date/Time APIs**: Both legacy `Date`/`Calendar` and modern `java.time` package
- **Custom Graphics**: Advanced 2D drawing with `Graphics2D`
- **Event Handling**: ActionListeners for UI controls

## Requirements

- Java 8 or higher
- Swing and AWT libraries (included in standard Java)

## Building and Running

### Compilation

To compile all clock implementations:

```
cd "d:\Programing\Experiment\lab2\Digital Clock"
javac -d build Src/*.java
```

This will compile all Java source files and place the resulting class files in the `build` directory.

### Running Individual Implementations

After compilation, you can run any of the clock implementations from the build directory:

Basic Digital Clock:
```
cd "d:\Programing\Experiment\lab2\Digital Clock"
java -cp build DigitalClock
```

Analog Clock:
```
cd "d:\Programing\Experiment\lab2\Digital Clock"
java -cp build AnalogClock
```

World Time Zones Clock:
```
cd "d:\Programing\Experiment\lab2\Digital Clock"
java -cp build WorldTimeZones
```

Advanced Digital Clock:
```
cd "d:\Programing\Experiment\lab2\Digital Clock"
java -cp build AdvancedDigitalClock
```

### Using the Build Script

For convenience, a batch script (`build.bat`) is provided to simplify building and running the project:

```
# Compile all Java files
build.bat compile

# Run the Advanced Digital Clock (default)
build.bat run

# Run specific implementations
build.bat run-basic     # Run Basic Digital Clock
build.bat run-analog    # Run Analog Clock
build.bat run-world     # Run World Time Zones Clock
build.bat run-advanced  # Run Advanced Digital Clock

# Clean the build directory
build.bat clean
```

## Screenshots

(Add screenshots here to showcase the application)

## Future Improvements

- Add alarm functionality
- Implement themes and customization options
- Add calendar integration
- Create a stopwatch and timer feature
- Persist user preferences between sessions
- Add sound effects for clock operations
