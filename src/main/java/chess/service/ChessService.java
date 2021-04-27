package chess.service;

import chess.domain.ChessGame;
import chess.domain.board.Board;
import chess.domain.board.Path;
import chess.domain.command.Commands;
import chess.domain.utils.PieceInitializer;
import chess.dto.CommandDto;
import chess.dto.GameInfoDto;
import chess.dto.RoomDto;
import chess.dto.UserInfoDto;
import chess.repository.CommandRepository;
import chess.repository.RoomRepository;
import chess.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import spark.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChessService {
    private final CommandRepository commandRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ChessService(CommandRepository commandRepository, RoomRepository roomRepository,
                        UserRepository userRepository) {
        this.commandRepository = commandRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<RoomDto> loadActiveRooms() {
        return rooms();
    }

    private List<RoomDto> rooms() {
        return new ArrayList<>(roomRepository.selectActiveRooms());
    }

    public GameInfoDto gameInfo(String id) {
        final List<CommandDto> commands = lastState(id);
        if (commands.isEmpty()) {
            return initialGameInfo();
        }
        return continuedGameInfo(id, commands);
    }

    public GameInfoDto initialGameInfo() {
        return new GameInfoDto(new ChessGame(Board.of(PieceInitializer.pieceInfo())));
    }

    public GameInfoDto continuedGameInfo(String id, List<CommandDto> commands) {
        ChessGame chessGame = restore(commands);
        if (chessGame.isEnd()) {
            updateToEnd(id);
        }
        return new GameInfoDto(chessGame);
    }

    private ChessGame restore(List<CommandDto> commands) {
        ChessGame chessGame = new ChessGame(Board.of(PieceInitializer.pieceInfo()));
        chessGame.makeBoardStateOf(commands);
        return chessGame;
    }

    private List<CommandDto> lastState(String id) {
        return commandRepository.selectAllCommandsByRoomId(id);
    }

    public void move(String id, String command, UserInfoDto userInfoDto) {
        moveValidation(id);
        ChessGame chessGame = restore(lastState(id));

        final Path path = new Path(new Commands(command).path());
        chessGame.isCorrectTurnBetween(path.source(), userRepository.findTeamByPassword(userInfoDto));

        chessGame.moveAs(path);
        updateMoveInfo(command, id);
    }

    private void moveValidation(String id) {
        if (checkRoomEnd(id)) {
            throw new IllegalArgumentException("이미 종료된 게임입니다😞");
        }
        if (!checkRoomFull(id)) {
            throw new IllegalArgumentException("흑팀 참가자가 아직 입장하지 않았습니다😞");
        }
    }

    public String addRoom(String name) {
        final int id = roomRepository.insert(name);
        return String.valueOf(id);
    }

    private void updateMoveInfo(String command, String roomId) {
        if (StringUtils.isNotEmpty(roomId)) {
            flushCommands(command, roomId);
        }
    }

    public void updateToEnd(String roomId) {
        roomRepository.updateToEnd(roomId);
    }

    private void flushCommands(String command, String roomId) {
        try {
            commandRepository.insert(new CommandDto(command), roomId);
        } catch (DataAccessException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addUser(String roomId, String password, String team) {
        userRepository.insert(roomId, password, team);
    }

    public boolean checkRoomFull(String roomId) {
        return roomRepository.checkRoomIsFull(roomId);
    }

    public void updateToFull(String roomId, String password) {
        updateValidation(roomId, password);
        roomRepository.updateToFull(roomId);
    }

    private void updateValidation(String roomId, String password) {
        if (checkRoomFull(roomId)) {
            throw new IllegalArgumentException("이미 꽉 찬 방이에요 😅");
        }
        if (checkSamePassword(roomId, password)) {
            throw new IllegalArgumentException("굉장하군요. 백팀 참가자와 같은 비밀번호를 입력했어요😲 다른 비밀번호로 부탁해요~");
        }
    }

    public boolean checkSamePassword(String roomId, String password) {
        return userRepository.isExist(roomId, password);
    }

    public boolean checkRoomEnd(String roomId) {
        return roomRepository.isEnd(roomId);
    }
}