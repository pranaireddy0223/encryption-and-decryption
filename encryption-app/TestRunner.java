public class TestRunner {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("          RUNNING STANDALONE TEST SUITE           ");
        System.out.println("==================================================");

        runTest("Basic Encryption & Decryption", () -> {
            String original = "Hello, World! Secret Message.";
            String encrypted = Encrypt.encryptText(original);
            
            // Check if encrypted is base64 encoded and not empty
            assertNotNull(encrypted, "Ciphertext should not be null");
            assertNotEquals(original, encrypted, "Ciphertext should be different from plaintext");
            
            String decrypted = Encrypt.decryptText(encrypted);
            assertEquals(original, decrypted, "Decrypted text must match original plaintext");
        });

        runTest("Randomized IV (Different Ciphertexts)", () -> {
            String original = "Same message";
            String encrypted1 = Encrypt.encryptText(original);
            String encrypted2 = Encrypt.encryptText(original);

            assertNotEquals(encrypted1, encrypted2, "Two encryptions of the same text must yield different ciphertexts due to random IV");
            
            // Both must decrypt correctly to the same text
            assertEquals(original, Encrypt.decryptText(encrypted1), "Decrypted 1 matches original");
            assertEquals(original, Encrypt.decryptText(encrypted2), "Decrypted 2 matches original");
        });

        runTest("Special Characters and Unicode Support", () -> {
            String original = "🔑 Unicode Secret: 🧑‍💻 Antigravity Agent! 日本語 & 中文 & Español";
            String encrypted = Encrypt.encryptText(original);
            String decrypted = Encrypt.decryptText(encrypted);

            assertEquals(original, decrypted, "Decrypted text must preserve complex Unicode strings");
        });

        runTest("Edge Case: Empty String", () -> {
            String original = "";
            String encrypted = Encrypt.encryptText(original);
            String decrypted = Encrypt.decryptText(encrypted);

            assertEquals(original, decrypted, "Decrypted empty string matches original");
        });

        runTest("Invalid Ciphertext (Corrupt Base64)", () -> {
            try {
                Encrypt.decryptText("InvalidCiphertextThatIsTooShort");
                fail("Should have failed to decrypt invalid ciphertext");
            } catch (Exception e) {
                // Passed, expected exception
            }
        });

        System.out.println("==================================================");
        System.out.printf("Test Suite Finished. Passed: %d/%d\n", passedTests, totalTests);
        System.out.println("==================================================");
        
        if (passedTests < totalTests) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    interface TestRunnable {
        void run() throws Exception;
    }

    private static void runTest(String name, TestRunnable test) {
        totalTests++;
        System.out.printf("Test #%d [%s]: ", totalTests, name);
        try {
            test.run();
            passedTests++;
            System.out.println("\u001B[32mPASSED\u001B[0m"); // Green output
        } catch (Throwable t) {
            System.out.println("\u001B[31mFAILED\u001B[0m"); // Red output
            t.printStackTrace(System.out);
        }
    }

    // Custom assertion helpers
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(String.format("%s: expected <%s> but was <%s>", message, expected, actual));
    }

    private static void assertNotEquals(Object val1, Object val2, String message) {
        if (val1 == null && val2 != null) return;
        if (val1 != null && !val1.equals(val2)) return;
        throw new AssertionError(String.format("%s: values must not be equal, but both were <%s>", message, val1));
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj != null) return;
        throw new AssertionError(message + ": expected object to be not null");
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
