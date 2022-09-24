# simple-forgetting-map

- A simple class to provide a key-value map with a fixed maximum size
    - An entry can be added with `add(key, value)`
    - An entry can be read using `find(key)`
- When the map is added to beyond its maximum size, the least-used entry is removed
    - Least-used is defined as the entry which has been accessed the least number of times using the `find(key)` method
    - In the event of multiple least-used entries, the oldest (earliest-added) entry will be removed
