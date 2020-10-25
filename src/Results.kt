package io.github.jmoore34

import io.github.jmoore34.repository.Poll
import io.github.jmoore34.repository.Votes
import java.util.*
import kotlin.collections.LinkedHashSet

inline class Round(val votesPerCandidate: Array<Int>)
class Results(val rounds: List<Round>, val winnerCandidateIndex: Int, val eliminatedCandidateIndices: LinkedHashSet<Int>)

typealias CandidateQueue = Queue<Int> // e.g. first choice is candidate index 2, second choice is candidate index 5, etc.

typealias CandidateQ = Array<Int>

fun calculateWinner(poll: Poll): Results {
    // Step 0: Deserialize the vote data
    val numCandidates = poll.candidates.length // indices go from 0..numCandidates-1
    val ballots: List<CandidateQueue> = poll.votes.map {
        val q: CandidateQueue = ArrayDeque<Int>()
        it.choices.split(Votes.choicesDelimiter).forEach {
            q.add(it.toInt())
        }
        q
    }

    var winnerCandidateIndex: Int? = null
    var currentRoundIndex = 0
    val rounds = mutableListOf<Round>()
    val eliminatedCandidateIndices = LinkedHashSet<Int>()

    while (winnerCandidateIndex == null) {
        // Start of a new round
        // First, initialize the vote counts
        // to 0 for round 0
        // or to the previous round's counts for all later rounds
        // setting the candidate that was eliminated in the previous round to 0 points

        if (currentRoundIndex == 0) {
            rounds.add(Round(
                    votesPerCandidate = Array<Int>(numCandidates) {0},
            ))
        }
        else {
            rounds.add(Round(
                    votesPerCandidate = rounds[currentRoundIndex - 1].votesPerCandidate.clone(),
            ))
            eliminatedCandidateIndices.forEach {
                // eliminated candidates should graphically display zero votes after they've been eliminated
                // to make it look like the votes have been moved
                rounds[currentRoundIndex].votesPerCandidate[it] = 0
            }
        }

        // Now, for each ballot, look at their first choice that hasn't yet been eliminated
        // then, we tally it
        ballots.forEach {
            // first round only: everyone's first choice pick is tallied towards that candidate.
            // we also remove that choice from their ballot as to not double count later
            if (currentRoundIndex == 0 && !it.isEmpty())
                rounds[currentRoundIndex].votesPerCandidate[it.remove()]++
            else {
                // all other rounds:
                // if the head choices of a voter are eliminated, then their vote
                // needs to be transferred. so, we find the first candidate they voted
                // for that is not eliminated.

                // 1. remove eliminated candidates
                while (!it.isEmpty() && it.peek() in eliminatedCandidateIndices)
                    it.remove() // remove/skip eliminated candidates
                // 2. add head candidate to their tally
                if (!it.isEmpty())
                    rounds[currentRoundIndex].votesPerCandidate[it.remove()]++
            }
        }

        fun Array<Int>.indexOfMin(): Int {
            var minIndex = 0
            this.forEachIndexed { i, value ->
                if (value < this[minIndex])
                    minIndex = i
            }
            return minIndex
        }
        fun Array<Int>.indexOfMax(): Int {
            var maxIndex = 0
            this.forEachIndexed { i, value ->
                if (value > this[maxIndex])
                    maxIndex = i
            }
            return maxIndex
        }

        // Now, we set the eliminated candidate to the one with the least votes
        eliminatedCandidateIndices.add(rounds[currentRoundIndex].votesPerCandidate.indexOfMin())

        val leadingCandidateIndex = rounds[currentRoundIndex].votesPerCandidate.indexOfMax()
        if (leadingCandidateIndex >= 0.5 * ballots.size)
            winnerCandidateIndex = leadingCandidateIndex

        currentRoundIndex++
    }

    return Results(rounds = rounds, winnerCandidateIndex = winnerCandidateIndex, eliminatedCandidateIndices = eliminatedCandidateIndices)
}