import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

class CryptoUtil {
    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing error: " + e.getMessage());
        }
    }
}

class Transaction {
    String from, to;
    int amount, incentive;

    Transaction(String from, String to, int amount, int incentive) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.incentive = incentive;
    }

    public String getTransactionHash() {
        return CryptoUtil.hash(from + incentive + to + amount);
    }

    @Override
    public String toString() {
        return "[\"" + from + "\", \"" + to + "\", " + amount + ", " + incentive + "]";
    }
}

class Miner {
    String id;
    int computationScore;
    int[] blockHashScoreArray;

    Miner(String id, int computationScore, int[] blockHashScoreArray) {
        this.id = id.toUpperCase(); // Ensuring miner ID is in uppercase
        this.computationScore = computationScore;
        this.blockHashScoreArray = blockHashScoreArray;
    }

    public int getBlockSealingScore(int blockNumber) {
        return computationScore * blockHashScoreArray[blockNumber % 8];
    }
}

class Block {
    int blockNumber;
    String prevBlockHash;
    List<Transaction> transactions;
    String merkleRoot;
    String blockHash;
    int nonce;
    Miner selectedMiner;

    Block(int blockNumber, String prevBlockHash, List<Transaction> transactions, Miner selectedMiner) {
        this.blockNumber = blockNumber;
        this.prevBlockHash = prevBlockHash;
        this.transactions = new ArrayList<>(transactions);
        this.merkleRoot = computeMerkleRoot(transactions);
        this.blockHash = CryptoUtil.hash(prevBlockHash + blockNumber + merkleRoot);
        this.nonce = computeNonce();
        this.selectedMiner = selectedMiner;
    }

    private String computeMerkleRoot(List<Transaction> transactions) {
        List<String> hashes = new ArrayList<>();
        for (Transaction txn : transactions) {
            hashes.add(txn.getTransactionHash());
        }
        while (hashes.size() > 1) {
            List<String> newHashes = new ArrayList<>();
            for (int i = 0; i < hashes.size(); i += 2) {
                if (i + 1 < hashes.size()) {
                    newHashes.add(CryptoUtil.hash(hashes.get(i) + hashes.get(i + 1)));
                } else {
                    newHashes.add(hashes.get(i));
                }
            }
            hashes = newHashes;
        }
        return hashes.isEmpty() ? "" : hashes.get(0);
    }

    private int computeNonce() {
        int nonce = 0;
        while (true) {
            String hashValue = CryptoUtil.hash(blockHash + nonce);
            if (hashValue.endsWith("0")) {
                return nonce;
            }
            nonce++;
        }
    }

    @Override
    public String toString() {
        return blockNumber + "\n" + blockHash + "\n" + transactions + "\n" + merkleRoot + "\n" + nonce + " " + selectedMiner.id;
    }
}

public class Bonthu{
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Read accounts and balances
        int numAcc = Integer.parseInt(scanner.nextLine());
        Map<String, Integer> balances = new HashMap<>();
        for (int i = 0; i < numAcc; i++) {
            String[] parts = scanner.nextLine().split(" ");
            balances.put(parts[0], Integer.parseInt(parts[1]));
        }

        // Read transactions
        int numTxn = Integer.parseInt(scanner.nextLine());
        List<Transaction> unconfirmedTransactions = new ArrayList<>();
        for (int i = 0; i < numTxn; i++) {
            String[] parts = scanner.nextLine().split(" ");
            unconfirmedTransactions.add(new Transaction(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        }

        // Read block reward
        int blockReward = Integer.parseInt(scanner.nextLine());

        // Read miners
        int numMiners = Integer.parseInt(scanner.nextLine());
        List<Miner> miners = new ArrayList<>();
        for (int i = 0; i < numMiners; i++) {
            String[] parts = scanner.nextLine().split(" ");
            String id = parts[0].toUpperCase(); // Ensure uppercase miner ID
            int computationScore = Integer.parseInt(parts[1]);
            int[] blockHashScoreArray = new int[8];
            for (int j = 0; j < 8; j++) {
                blockHashScoreArray[j] = Integer.parseInt(parts[j + 2]);
            }
            miners.add(new Miner(id, computationScore, blockHashScoreArray));
        }
        scanner.close();

        // Sort transactions based on incentive, then by receiver account lexicographically
        unconfirmedTransactions.sort((a, b) -> {
            if (b.incentive != a.incentive) return Integer.compare(b.incentive, a.incentive);
            return a.to.compareTo(b.to);
        });

        String prevBlockHash = "0";
        int blockNumber = 1;
        List<Transaction> currentBlockTxns = new ArrayList<>();

        for (Transaction txn : unconfirmedTransactions) {
            if (balances.getOrDefault(txn.from, 0) >= txn.amount) {
                balances.put(txn.from, balances.get(txn.from) - txn.amount);
                balances.put(txn.to, balances.getOrDefault(txn.to, 0) + txn.amount);
                currentBlockTxns.add(txn);
            }
            if (currentBlockTxns.size() == 4) {
                Miner selectedMiner = selectMiner(miners, blockNumber);
                balances.put(selectedMiner.id, balances.getOrDefault(selectedMiner.id, 0) + blockReward); // Add block reward
                Block block = new Block(blockNumber, prevBlockHash, currentBlockTxns, selectedMiner);
                System.out.println(block);
                prevBlockHash = block.blockHash;
                blockNumber++;
                currentBlockTxns.clear();
            }
        }
        if (!currentBlockTxns.isEmpty()) {
            Miner selectedMiner = selectMiner(miners, blockNumber);
            balances.put(selectedMiner.id, balances.getOrDefault(selectedMiner.id, 0) + blockReward); // Add block reward
            Block block = new Block(blockNumber, prevBlockHash, currentBlockTxns, selectedMiner);
            System.out.println(block);
        }
    }

    private static Miner selectMiner(List<Miner> miners, int blockNumber) {
        return miners.stream()
                .max(Comparator.comparingInt(miner -> miner.getBlockSealingScore(blockNumber)))
                .orElse(null);
    }
}

