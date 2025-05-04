
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class BlockChain {

    static class Transaction {

        String from, to;
        int amount, extra;

        Transaction(String f, String t, int a, int e) {
            from = f;
            to = t;
            amount = a;
            extra = e;
        }

        @Override
        public String toString() {
            return String.format("[\"%s\", \"%s\", %d, %d]", from, to, amount, extra);
        }
    }

    static class Block {

        int blockNumber;
        String prevHash, merkleRoot, blockHash;
        List<Transaction> transactions = new ArrayList<>();

        Block(int num, String prevHash, List<Transaction> txns) {
            this.blockNumber = num;
            this.prevHash = prevHash;
            this.transactions = txns;
            this.merkleRoot = buildMerkleRoot(txns);
            this.blockHash = hash(prevHash + blockNumber + merkleRoot);
        }

        void printBlock() {
            System.out.println(blockNumber);
            System.out.println(blockHash);
            System.out.println(transactions);
            System.out.println(merkleRoot);
        }
    }

    static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String hashTxn(Transaction t) {
        return hash(t.from + t.to + t.amount);
    }

    static String buildMerkleRoot(List<Transaction> txns) {
        List<String> hashes = new ArrayList<>();
        for (Transaction t : txns) {
            hashes.add(hashTxn(t));
        }

        while (hashes.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < hashes.size(); i += 2) {
                if (i + 1 < hashes.size()) {
                    next.add(hash(hashes.get(i) + hashes.get(i + 1))); 
                }else {
                    next.add(hash(hashes.get(i)));
                }
            }
            hashes = next;
        }

        return hashes.isEmpty() ? hash("") : hashes.get(0);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int numAcc = Integer.parseInt(sc.nextLine());
        Map<String, Integer> balances = new HashMap<>();

        for (int i = 0; i < numAcc; i++) {
            String[] parts = sc.nextLine().split(" ");
            balances.put(parts[0], Integer.parseInt(parts[1]));
        }

        int numTxn = Integer.parseInt(sc.nextLine());
        List<Transaction> allTxns = new ArrayList<>();

        for (int i = 0; i < numTxn; i++) {
            String[] p = sc.nextLine().split(" ");
            allTxns.add(new Transaction(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3])));
        }

        List<Transaction> blockTxns = new ArrayList<>();
        List<Block> blockchain = new ArrayList<>();
        int blockNum = 1;
        String prevHash = "0";

        for (Transaction t : allTxns) {
            int fromBal = balances.getOrDefault(t.from, 0);
            if (fromBal >= t.amount) {
                balances.put(t.from, fromBal - t.amount);
                balances.put(t.to, balances.getOrDefault(t.to, 0) + t.amount);
                blockTxns.add(t);

                if (blockTxns.size() == 3) {
                    Block b = new Block(blockNum++, prevHash, new ArrayList<>(blockTxns));
                    blockchain.add(b);
                    prevHash = b.blockHash;
                    blockTxns.clear();
                }
            }
        }

        if (!blockTxns.isEmpty()) {
            Block b = new Block(blockNum, prevHash, blockTxns);
            blockchain.add(b);
        }

        for (Block b : blockchain) {
            b.printBlock();
        }
    }
}
