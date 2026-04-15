package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.WordDao
import com.example.unscramble.data.WordEntity
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(private val wordDao: WordDao) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    var addWords by mutableStateOf("")
        private set
    var userGuess by mutableStateOf("")
        private set

    private var usedWords: MutableSet<String> = mutableSetOf()
    private lateinit var currentWord: String
    private var wordsFromDb: List<String> = emptyList()
    private var allAvailableWords: List<String> = allWords.toList()

    init {
        viewModelScope.launch {
            wordDao.getAllWords().collect { wordsFromDb ->
                allAvailableWords = (allWords + wordsFromDb).toList()

                if (allAvailableWords.isNotEmpty() && _uiState.value.currentScrambledWord.isEmpty()) {
                    resetGame()
                }
            }
        }
    }

    fun saveNewWord() {
        if (addWords.isNotBlank()) {
            viewModelScope.launch {
                wordDao.insert(WordEntity(word = addWords.lowercase().trim()))
                addWords = ""
            }
        }
    }

    fun resetGame() {
        usedWords.clear()
        if (allAvailableWords.isNotEmpty()) {
            _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
        }
    }

    fun updateUserGuess(guessedWord: String){
        userGuess = guessedWord
    }

    fun updateAddWords(word: String) {
        addWords = word
    }

    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size >= MAX_NO_OF_WORDS || usedWords.size >= allAvailableWords.size){
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = false, score = updatedScore, isGameOver = true)
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    currentWordCount = currentState.currentWordCount.inc(),
                    score = updatedScore
                )
            }
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word && word.length > 1) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    private fun pickRandomWordAndShuffle(): String {
        currentWord = allAvailableWords.random()
        return if (usedWords.contains(currentWord)) {
            pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            shuffleCurrentWord(currentWord)
        }
    }
}