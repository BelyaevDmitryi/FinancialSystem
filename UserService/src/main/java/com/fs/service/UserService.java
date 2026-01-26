package com.fs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fs.domain.Position;
import com.fs.domain.Stock;
import com.fs.domain.User;
import com.fs.dto.PositionWithStockDto;
import com.fs.dto.PositionsDto;
import com.fs.dto.StocksDto;
import com.fs.dto.TickersDto;
import com.fs.dto.UpdateProfileDto;
import com.fs.dto.UserDtoCreate;
import com.fs.dto.UserProfileDto;
import com.fs.exception.UserAlreadyExistException;
import com.fs.exception.UserNotFoundException;
import com.fs.repository.StockRepository;
import com.fs.repository.UserRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final StockApiService stockApiService;
    private final FileStorageService fileStorageService;

    @Transactional
    public User createUser(UserDtoCreate userDtoCreate) {
        if(userRepository.existsByName(userDtoCreate.getName())) {
            throw new UserAlreadyExistException("User with this name already exists. Try another name.");
        }

        Set<String> notExistTickers = new HashSet<>();
        Set<Position> positions = new HashSet<>();
        for(Position position : userDtoCreate.getPortfolio()) {
            if (!stockRepository.existsByTicker(position.getTicker())) {
                notExistTickers.add(position.getTicker());
            }
            Position pos = new Position();
            pos.setTicker(position.getTicker());
            pos.setQuantity(position.getQuantity());
            positions.add(pos);
        }

        if (!notExistTickers.isEmpty()) {
            StocksDto stocksDto = stockApiService.getStocksByTickers(new TickersDto(notExistTickers));
            stockRepository.saveAll(stocksDto.getStocks());
        }
        
        User newUser = new User();
        newUser.setName(userDtoCreate.getName());
        newUser.setNickname(userDtoCreate.getName()); // По умолчанию nickname = name
        newUser.setPassword(null); // Password will be set during signup
        // Дефолтный аватар не устанавливаем - будет использоваться локальная картинка
        newUser.setAvatarUrl(null);
        newUser.setRoles(new HashSet<>());
        newUser = userRepository.save(newUser);
        
        // Устанавливаем связь с пользователем для позиций
        for (Position position : positions) {
            position.setUser(newUser);
        }
        newUser.setPortfolio(positions);
        return userRepository.save(newUser);
    }

    @Transactional
    public User addStocksToUser(Long id, PositionsDto positionsDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        Set<Position> userPositions = user.getPortfolio();
        
        Set<Position> addPositions = new HashSet<>();
        for (Position pos : positionsDto.getPositions()) {
            Position existingPos = userPositions.stream()
                    .filter(p -> p.getTicker().equals(pos.getTicker()))
                    .findFirst()
                    .orElse(null);
            
            if (existingPos != null) {
                existingPos.setQuantity(pos.getQuantity());
            } else {
                Position newPos = new Position();
                newPos.setUser(user);
                newPos.setTicker(pos.getTicker());
                newPos.setQuantity(pos.getQuantity());
                addPositions.add(newPos);
            }
        }
        
        if(!addPositions.isEmpty()) {
            Set<String> notExistTickers = addPositions.stream()
                    .map(Position::getTicker)
                    .filter(ticker -> !stockRepository.existsByTicker(ticker))
                    .collect(Collectors.toSet());
            
            if (!notExistTickers.isEmpty()) {
                TickersDto tickersDto = new TickersDto(notExistTickers);
                StocksDto stocksDto = stockApiService.getStocksByTickers(tickersDto);
                stockRepository.saveAll(stocksDto.getStocks());
            }
            
            userPositions.addAll(addPositions);
        }

        return userRepository.save(user);
    }
    
    @Transactional
    public User addStocksToUser(String id, PositionsDto positionsDto) {
        try {
            Long userId = Long.parseLong(id);
            return addStocksToUser(userId, positionsDto);
        } catch (NumberFormatException e) {
            throw new UserNotFoundException("Invalid user id format: " + id);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found. Try another Id."));
    }
    
    public User getUserById(String id) {
        try {
            Long userId = Long.parseLong(id);
            return getUserById(userId);
        } catch (NumberFormatException e) {
            throw new UserNotFoundException("Invalid user id format: " + id);
        }
    }

    @Transactional
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }
    
    public void deleteUserById(String id) {
        try {
            Long userId = Long.parseLong(id);
            deleteUserById(userId);
        } catch (NumberFormatException e) {
            throw new UserNotFoundException("Invalid user id format: " + id);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Long getTotalUsersCount() {
        return userRepository.count();
    }

    public List<PositionWithStockDto> getUserPositions(String userId) {
        User user = getUserById(userId);
        return user.getPortfolio().stream()
                .map(position -> {
                    Stock stock = stockRepository.findByTicker(position.getTicker())
                            .orElseThrow(() -> new RuntimeException("Stock not found for ticker: " + position.getTicker()));
                    return new PositionWithStockDto(
                            stock.getFigi(),
                            stock.getTicker(),
                            stock.getName(),
                            BigDecimal.valueOf(position.getQuantity()),
                            stock.getCurrency().name()
                    );
                })
                .collect(Collectors.toList());
    }

    public UserProfileDto getUserProfile(String userId) {
        User user = getUserById(userId);
        return new UserProfileDto(
                user.getId(),
                user.getName(), // username (логин)
                user.getNickname() != null ? user.getNickname() : user.getName(), // nickname или name по умолчанию
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "/images/default-avatar.png", // дефолтный аватар если не указан
                user.getRoles().stream().toList()
        );
    }

    @Transactional
    public UserProfileDto updateUserProfile(String userId, UpdateProfileDto updateProfileDto) {
        User user = getUserById(userId);
        
        // Обновление логина (username)
        if (updateProfileDto.getUsername() != null && !updateProfileDto.getUsername().trim().isEmpty()) {
            String newUsername = updateProfileDto.getUsername().trim();
            // Проверяем, что новый логин не занят другим пользователем
            if (userRepository.existsByName(newUsername) && !user.getName().equals(newUsername)) {
                throw new UserAlreadyExistException("Пользователь с таким логином уже существует");
            }
            user.setName(newUsername);
        }
        
        // Обновление никнейма
        if (updateProfileDto.getNickname() != null && !updateProfileDto.getNickname().trim().isEmpty()) {
            user.setNickname(updateProfileDto.getNickname().trim());
        }
        
        // Обновление аватара
        if (updateProfileDto.getAvatarUrl() != null) {
            String oldAvatarUrl = user.getAvatarUrl();
            String avatarUrl = updateProfileDto.getAvatarUrl().trim();
            
            // Удаляем старый файл, если он был загружен (не URL и не дефолтный)
            if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/uploads/avatars/")) {
                fileStorageService.deleteAvatar(oldAvatarUrl);
            }
            
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }
        
        User updatedUser = userRepository.save(user);
        return new UserProfileDto(
                updatedUser.getId(),
                updatedUser.getName(), // username (логин)
                updatedUser.getNickname() != null ? updatedUser.getNickname() : updatedUser.getName(), // nickname
                updatedUser.getAvatarUrl() != null ? updatedUser.getAvatarUrl() : "/images/default-avatar.png", // дефолтный аватар если не указан
                updatedUser.getRoles().stream().toList()
        );
    }

    @Transactional
    public UserProfileDto updateUserAvatar(String userId, String avatarPath) {
        User user = getUserById(userId);
        
        // Удаляем старый файл, если он был загружен (не URL и не дефолтный)
        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/uploads/avatars/")) {
            fileStorageService.deleteAvatar(oldAvatarUrl);
        }
        
        user.setAvatarUrl(avatarPath);
        User updatedUser = userRepository.save(user);
        
        return new UserProfileDto(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getNickname() != null ? updatedUser.getNickname() : updatedUser.getName(),
                updatedUser.getAvatarUrl() != null ? updatedUser.getAvatarUrl() : "/images/default-avatar.png",
                updatedUser.getRoles().stream().toList()
        );
    }

}
