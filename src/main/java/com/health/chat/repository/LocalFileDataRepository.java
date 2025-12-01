package com.health.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.TankaPoem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local file system implementation of DataRepository.
 * Stores data in JSON files organized by user and date.
 */
public class LocalFileDataRepository implements DataRepository {
    
    private static final Logger LOGGER = Logger.getLogger(LocalFileDataRepository.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final String baseDirectory;
    private final ObjectMapper objectMapper;
    
    public LocalFileDataRepository(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Create base directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(baseDirectory));
            LOGGER.info("Local data repository initialized at: " + baseDirectory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create base directory: " + baseDirectory, e);
        }
    }
    
    @Override
    public void saveHealthData(String userId, HealthData data) {
        try {
            Path filePath = getHealthDataPath(userId, data.getDate());
            ensureDirectoryExists(filePath.getParent());
            
            // Read existing data for the day
            List<HealthData> dailyData = new ArrayList<>();
            if (Files.exists(filePath)) {
                HealthData[] existing = objectMapper.readValue(filePath.toFile(), HealthData[].class);
                dailyData.addAll(List.of(existing));
            }
            
            // Add new data
            dailyData.add(data);
            
            // Write back
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), dailyData);
            LOGGER.info("Saved health data for user: " + userId + ", date: " + data.getDate());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save health data", e);
            throw new RuntimeException("Failed to save health data", e);
        }
    }
    
    @Override
    public List<HealthData> getHealthDataByDateRange(String userId, LocalDate start, LocalDate end) {
        List<HealthData> result = new ArrayList<>();
        
        try {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                Path filePath = getHealthDataPath(userId, current);
                
                if (Files.exists(filePath)) {
                    HealthData[] dailyData = objectMapper.readValue(filePath.toFile(), HealthData[].class);
                    result.addAll(List.of(dailyData));
                }
                
                current = current.plusDays(1);
            }
            
            LOGGER.info("Retrieved " + result.size() + " health data entries for user: " + userId);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve health data", e);
        }
        
        return result;
    }
    
    @Override
    public void saveNutritionInfo(String userId, LocalDate date, NutritionInfo info) {
        try {
            Path filePath = getNutritionPath(userId, date);
            ensureDirectoryExists(filePath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), info);
            LOGGER.info("Saved nutrition info for user: " + userId + ", date: " + date);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save nutrition info", e);
            throw new RuntimeException("Failed to save nutrition info", e);
        }
    }
    
    @Override
    public void saveMentalState(String userId, LocalDate date, MentalState state) {
        try {
            Path filePath = getMentalStatePath(userId, date);
            ensureDirectoryExists(filePath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), state);
            LOGGER.info("Saved mental state for user: " + userId + ", date: " + date);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save mental state", e);
            throw new RuntimeException("Failed to save mental state", e);
        }
    }
    
    @Override
    public void saveTanka(String userId, TankaPoem tanka) {
        try {
            Path filePath = getTankaPath(userId, tanka.getDate());
            ensureDirectoryExists(filePath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), tanka);
            LOGGER.info("Saved tanka for user: " + userId + ", date: " + tanka.getDate());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save tanka", e);
            throw new RuntimeException("Failed to save tanka", e);
        }
    }
    
    @Override
    public NutritionInfo getNutritionInfo(String userId, LocalDate date) {
        try {
            Path filePath = getNutritionPath(userId, date);
            
            if (Files.exists(filePath)) {
                return objectMapper.readValue(filePath.toFile(), NutritionInfo.class);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve nutrition info", e);
        }
        
        return null;
    }
    
    @Override
    public MentalState getMentalState(String userId, LocalDate date) {
        try {
            Path filePath = getMentalStatePath(userId, date);
            
            if (Files.exists(filePath)) {
                return objectMapper.readValue(filePath.toFile(), MentalState.class);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve mental state", e);
        }
        
        return null;
    }
    
    @Override
    public void saveUserProfile(com.health.chat.model.UserProfile profile) {
        try {
            Path filePath = getUserProfilePath(profile.getUserId());
            ensureDirectoryExists(filePath.getParent());
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), profile);
            LOGGER.info("Saved user profile for: " + profile.getUserId());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save user profile", e);
            throw new RuntimeException("Failed to save user profile", e);
        }
    }
    
    @Override
    public com.health.chat.model.UserProfile getUserProfile(String userId) {
        try {
            Path filePath = getUserProfilePath(userId);
            
            if (Files.exists(filePath)) {
                return objectMapper.readValue(filePath.toFile(), com.health.chat.model.UserProfile.class);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve user profile", e);
        }
        
        return null;
    }
    
    @Override
    public com.health.chat.model.UserProfile getUserProfileByUsername(String username) {
        // For local file system, we'll search through all user profiles
        try {
            Path usersDir = Paths.get(baseDirectory, "users");
            
            if (Files.exists(usersDir)) {
                return Files.list(usersDir)
                    .filter(Files::isDirectory)
                    .map(userDir -> {
                        try {
                            Path profilePath = userDir.resolve("profile.json");
                            if (Files.exists(profilePath)) {
                                return objectMapper.readValue(profilePath.toFile(), 
                                                            com.health.chat.model.UserProfile.class);
                            }
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to read profile: " + userDir, e);
                        }
                        return null;
                    })
                    .filter(profile -> profile != null && username.equals(profile.getUsername()))
                    .findFirst()
                    .orElse(null);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to search for user profile by username", e);
        }
        
        return null;
    }
    
    @Override
    public List<TankaPoem> getTankaHistory(String userId) {
        List<TankaPoem> result = new ArrayList<>();
        
        try {
            Path userTankaDir = Paths.get(baseDirectory, "users", userId, "tanka");
            
            if (Files.exists(userTankaDir)) {
                Files.walk(userTankaDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            TankaPoem tanka = objectMapper.readValue(p.toFile(), TankaPoem.class);
                            result.add(tanka);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to read tanka file: " + p, e);
                        }
                    });
            }
            
            // Sort by date descending
            result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            
            LOGGER.info("Retrieved " + result.size() + " tanka poems for user: " + userId);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve tanka history", e);
        }
        
        return result;
    }
    
    @Override
    public List<NutritionInfo> getNutritionInfoByDateRange(String userId, LocalDate start, LocalDate end) {
        List<NutritionInfo> result = new ArrayList<>();
        
        try {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                NutritionInfo info = getNutritionInfo(userId, current);
                if (info != null) {
                    result.add(info);
                }
                current = current.plusDays(1);
            }
            
            LOGGER.info("Retrieved " + result.size() + " nutrition entries for user: " + userId);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve nutrition info by date range", e);
        }
        
        return result;
    }
    
    @Override
    public List<MentalState> getMentalStatesByDateRange(String userId, LocalDate start, LocalDate end) {
        List<MentalState> result = new ArrayList<>();
        
        try {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                MentalState state = getMentalState(userId, current);
                if (state != null) {
                    result.add(state);
                }
                current = current.plusDays(1);
            }
            
            LOGGER.info("Retrieved " + result.size() + " mental states for user: " + userId);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve mental states by date range", e);
        }
        
        return result;
    }
    
    @Override
    public List<TankaPoem> getTankasByDateRange(String userId, LocalDate start, LocalDate end) {
        List<TankaPoem> allTankas = getTankaHistory(userId);
        List<TankaPoem> result = new ArrayList<>();
        
        for (TankaPoem tanka : allTankas) {
            LocalDate tankaDate = tanka.getDate();
            if (!tankaDate.isBefore(start) && !tankaDate.isAfter(end)) {
                result.add(tanka);
            }
        }
        
        LOGGER.info("Retrieved " + result.size() + " tankas for user: " + userId + " in date range");
        
        return result;
    }
    
    private Path getHealthDataPath(String userId, LocalDate date) {
        return Paths.get(baseDirectory, "users", userId, "health", 
                        String.valueOf(date.getYear()),
                        String.format("%02d", date.getMonthValue()),
                        date.format(DATE_FORMATTER) + ".json");
    }
    
    private Path getNutritionPath(String userId, LocalDate date) {
        return Paths.get(baseDirectory, "users", userId, "nutrition",
                        String.valueOf(date.getYear()),
                        String.format("%02d", date.getMonthValue()),
                        date.format(DATE_FORMATTER) + ".json");
    }
    
    private Path getMentalStatePath(String userId, LocalDate date) {
        return Paths.get(baseDirectory, "users", userId, "mental",
                        String.valueOf(date.getYear()),
                        String.format("%02d", date.getMonthValue()),
                        date.format(DATE_FORMATTER) + ".json");
    }
    
    private Path getTankaPath(String userId, LocalDate date) {
        return Paths.get(baseDirectory, "users", userId, "tanka",
                        String.valueOf(date.getYear()),
                        String.format("%02d", date.getMonthValue()),
                        date.format(DATE_FORMATTER) + ".json");
    }
    
    private Path getUserProfilePath(String userId) {
        return Paths.get(baseDirectory, "users", userId, "profile.json");
    }
    
    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}
