package com.kyn.qna.mcp;

import com.kyn.qna.model.Answer;
import com.kyn.qna.model.Question;
import io.modelcontextprotocol.core.Context;
import io.modelcontextprotocol.core.Fact;
import io.modelcontextprotocol.core.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpContextServiceTest {

    private McpContextService mcpContextService;
    private final String sessionId = "test-session-123";
    private final String userId = "user-abc";
    private final String techStack = "Java";
    private final String experience = "Senior";

    @BeforeEach
    void setUp() {
        mcpContextService = new McpContextService();
    }

    @Test
    void testInitializeContext() {
        mcpContextService.initializeContext(sessionId, techStack, experience, userId);
        Context context = mcpContextService.getContext(sessionId);

        assertNotNull(context, "Context should not be null after initialization.");
        assertNotNull(context.getIdentity(), "Identity should not be null.");
        assertEquals(userId, context.getIdentity().getSubject(), "User ID in Identity does not match.");

        assertNotNull(context.getFacts(), "Facts list should not be null.");
        assertEquals(2, context.getFacts().size(), "There should be two facts.");

        assertTrue(context.getFacts().stream().anyMatch(fact ->
                "technologyStack".equals(fact.getKey()) && techStack.equals(fact.getValue())),
                "Technology stack fact is missing or incorrect.");
        assertTrue(context.getFacts().stream().anyMatch(fact ->
                "experienceLevel".equals(fact.getKey()) && experience.equals(fact.getValue())),
                "Experience level fact is missing or incorrect.");
        
        assertNotNull(context.getMessages(), "Messages list should not be null and should be initialized.");
        assertTrue(context.getMessages().isEmpty(), "Messages list should be empty upon initialization.");
    }

    @Test
    void testGetContext_NotFound() {
        Context context = mcpContextService.getContext("non-existent-session");
        assertNull(context, "Getting context for a non-existent session ID should return null.");
    }

    @Test
    void testAddQuestionToContext() {
        mcpContextService.initializeContext(sessionId, techStack, experience, userId);

        Question question = new Question();
        question.setId("q1");
        question.setText("What is Spring Boot?");
        question.setTechnologyStack(techStack);
        question.setExperienceLevel(experience);
        question.setGeneratedBy("test-generator");
        question.setCreatedAt(LocalDateTime.now());

        mcpContextService.addQuestionToContext(sessionId, question);
        Context context = mcpContextService.getContext(sessionId);

        assertNotNull(context.getMessages(), "Messages list should not be null.");
        assertEquals(1, context.getMessages().size(), "Messages list should contain one message.");

        Message lastMessage = context.getMessages().get(0);
        assertEquals("assistant", lastMessage.getRole(), "Message role should be 'assistant' for questions.");
        assertEquals(question.getText(), lastMessage.getContent(), "Message content should match the question text.");
    }

    @Test
    void testAddQuestionToContext_ContextNotFound() {
        Question question = new Question();
        question.setText("Test question");
        // Attempt to add to a context that was never initialized
        assertDoesNotThrow(() -> mcpContextService.addQuestionToContext("non-existent-session-q", question),
                "Adding question to non-existent context should not throw (current behavior logs error).");
        
        // Verify context remains non-existent
        assertNull(mcpContextService.getContext("non-existent-session-q"));
    }
    
    @Test
    void testAddAnswerToContext() {
        mcpContextService.initializeContext(sessionId, techStack, experience, userId);

        Answer answer = new Answer();
        answer.setId("ans1");
        answer.setQuestionId("q1");
        answer.setText("Spring Boot is a framework.");
        answer.setAnsweredBy(userId);
        answer.setCreatedAt(LocalDateTime.now());

        mcpContextService.addAnswerToContext(sessionId, answer);
        Context context = mcpContextService.getContext(sessionId);

        assertNotNull(context.getMessages(), "Messages list should not be null.");
        assertEquals(1, context.getMessages().size(), "Messages list should contain one message.");

        Message lastMessage = context.getMessages().get(0);
        assertEquals("user", lastMessage.getRole(), "Message role should be 'user' for answers.");
        assertEquals(answer.getText(), lastMessage.getContent(), "Message content should match the answer text.");
    }
    
    @Test
    void testAddAnswerToContext_ContextNotFound() {
        Answer answer = new Answer();
        answer.setText("Test answer");
        assertDoesNotThrow(() -> mcpContextService.addAnswerToContext("non-existent-session-a", answer),
                "Adding answer to non-existent context should not throw (current behavior logs error).");
        assertNull(mcpContextService.getContext("non-existent-session-a"));
    }


    @Test
    void testGetFacts() {
        mcpContextService.initializeContext(sessionId, techStack, experience, userId);
        Context initialContext = mcpContextService.getContext(sessionId);
        List<Fact> initialFacts = initialContext.getFacts();

        List<Fact> retrievedFacts = mcpContextService.getFacts(sessionId);
        assertNotNull(retrievedFacts, "Retrieved facts list should not be null.");
        assertEquals(initialFacts.size(), retrievedFacts.size(), "Retrieved facts count should match initial facts.");
        assertTrue(retrievedFacts.containsAll(initialFacts) && initialFacts.containsAll(retrievedFacts),
                "Retrieved facts should be the same as initial facts.");
    }
    
    @Test
    void testGetFacts_ContextNotFound() {
        List<Fact> facts = mcpContextService.getFacts("non-existent-session");
        assertNotNull(facts, "Facts list should not be null even if context not found.");
        assertTrue(facts.isEmpty(), "Facts list should be empty if context not found.");
    }

    @Test
    void testGetMessages() {
        mcpContextService.initializeContext(sessionId, techStack, experience, userId);
        Question question = new Question();
        question.setText("Q1");
        mcpContextService.addQuestionToContext(sessionId, question);

        Answer answer = new Answer();
        answer.setText("A1");
        mcpContextService.addAnswerToContext(sessionId, answer);

        Context context = mcpContextService.getContext(sessionId);
        List<Message> initialMessages = context.getMessages();

        List<Message> retrievedMessages = mcpContextService.getMessages(sessionId);
        assertNotNull(retrievedMessages, "Retrieved messages list should not be null.");
        assertEquals(2, retrievedMessages.size(), "Should have two messages.");
        assertEquals(initialMessages.size(), retrievedMessages.size(), "Retrieved messages count should match initial messages.");
        assertTrue(retrievedMessages.containsAll(initialMessages) && initialMessages.containsAll(retrievedMessages),
                "Retrieved messages should be the same as initial messages.");
        assertEquals("assistant", retrievedMessages.get(0).getRole());
        assertEquals("Q1", retrievedMessages.get(0).getContent());
        assertEquals("user", retrievedMessages.get(1).getRole());
        assertEquals("A1", retrievedMessages.get(1).getContent());
    }

    @Test
    void testGetMessages_ContextNotFound() {
        List<Message> messages = mcpContextService.getMessages("non-existent-session");
        assertNotNull(messages, "Messages list should not be null even if context not found.");
        assertTrue(messages.isEmpty(), "Messages list should be empty if context not found.");
    }
}
