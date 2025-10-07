package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.domain.Position;
import ru.otus.hw.domain.User;
import ru.otus.hw.dto.PositionsDto;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.TickersDto;
import ru.otus.hw.dto.UserDtoCreate;
import ru.otus.hw.exception.UserAlreadyExistException;
import ru.otus.hw.exception.UserNotFoundException;
import ru.otus.hw.repository.StockRepository;
import ru.otus.hw.repository.UserRepository;

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

    @Transactional
    public User createUser(UserDtoCreate userDtoCreate) {
        if(userRepository.existsById(userDtoCreate.getId())) {
            throw new UserAlreadyExistException("User already exist. Try another Id.");
        }

        Set<String> notExistTickers = new HashSet<>();
        for(Position position : userDtoCreate.getPortfolio()) {
            if (!stockRepository.existsByTicker(position.getTicker())) {
                notExistTickers.add(position.getTicker());
            }
        }

        StocksDto stocksDto = stockApiService.getStocksByTickers(new TickersDto(notExistTickers));
        stockRepository.saveAll(stocksDto.getStocks());
        User newUser = new User(userDtoCreate.getId(),
                userDtoCreate.getName(),
                userDtoCreate.getPortfolio());
        return userRepository.save(newUser);
    }

    @Transactional
    public User addStocksToUser(String id, PositionsDto positionsDto) {
        Set<Position> addPositions = new HashSet<>();
        addPositions.addAll(positionsDto.getPositions());

        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        Set<Position> userPositions = user.getPortfolio();
        addPositions.removeAll(userPositions);
        if(!addPositions.isEmpty()) {
            TickersDto tickersDto = new TickersDto(
                    addPositions.stream().map(p -> p.getTicker()).collect(Collectors.toSet()));

            StocksDto stocksDto = stockApiService.getStocksByTickers(tickersDto);
            stockRepository.saveAll(stocksDto.getStocks());
        }

        positionsDto.getPositions().retainAll(userPositions);
        userPositions.removeAll(positionsDto.getPositions());
        userPositions.addAll(positionsDto.getPositions());
        if(!addPositions.isEmpty()) {
            userPositions.addAll(addPositions);
        }

        return userRepository.save(user);
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found. Try another Id."));
    }

    @Transactional
    public void deleteUserById(String id) {
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

}
