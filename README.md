# simple-forgetting-map
## A simple class to provide a key-value map which removes entries when increased beyond its maximum size
Written in Java using minimal third-party libraries

### Usage
- Created with constructor `new ForgettingMap<K, V>(maxSize)`
- An entry can be added with `add(key, value)`
- An entry can be read using `find(key)`
- When a new entry is added to the map which would increase the number of entries to beyond its maximum size, the least-used entry is removed
    - Least-used is defined as the entry which has been accessed the least number of times using the `find(key)` method
    - In the event of multiple least-used entries, the oldest (earliest-added) entry will be removed
    - Updating an existing entry is considered to be creating a new entry, i.e. if the most-used entry is overwritten with the `add(key, value)` method then this new entry will now be the least-used entry
    - This behaviour is verified by the unit and functional tests
- See JavaDoc for further detail
