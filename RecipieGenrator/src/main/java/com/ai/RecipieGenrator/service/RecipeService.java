package com.ai.RecipieGenrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecipeService {
    private final ChatModel chatModel;

    public RecipeService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    public String createRecipe(String rawIngredients ,
                               String rawCuisine ,
                               String rawDietaryRestrictions) {
        var correctedInput = autoCorrectInputs(rawIngredients, rawCuisine, rawDietaryRestrictions);
        var template = """
                I want to create master chef recipe using the following ingredients : {ingredients}.
                The cuisine type I prefer is {cuisine}.
                Please consider the following dietary restrictions: {dietaryRestrictions}.
                Please provide me with a detailed recipe including title , lists of ingredients and cooking instructions.
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(correctedInput);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    public String getSubstituteForIngredient(String missingIngredient, String recipeContext) {
        var template = """
            You are a professional chef. A user is cooking '{recipeContext}' and realized they don't have '{missingIngredient}'.
            
            Provide:
            1. The best immediate substitute.
            2. How it might change the final dish.
            3. A 'Pro-Tip' for using that substitute.
            
            Keep the response concise and helpful.
            """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of("missingIngredient", missingIngredient, "recipeContext", recipeContext);
        Prompt prompt = promptTemplate.create(params);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private Map<String, Object> autoCorrectInputs(String ingredients, String cuisine, String dietaryRestrictions) {
        var template = """
                You're an expert chef. The user might have provided some messy input so please clean it up:
                1. Fix typos like 'chiken' instead of 'chicken'
                2. If cuisine is vague, suggest one based on ingredients.
                3. Correct dietary restrictions if needed (e.g., 'no meat' -> 'Vegetarian').
                Return ONLY a JSON object with these keys: "ingredients", "cuisine", "dietaryRestrictions".
                Do not include any markdown formatting like ```json or extra text.
                Inputs to clean:
                - Ingredients: {ingredients}
                - Cuisine: {cuisine}
                - Dietary: {dietary}
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "ingredients", ingredients,
                "cuisine", cuisine,
                "dietary", dietaryRestrictions
        ));
        String openAiResp = chatModel.call(prompt).getResult().getOutput().getText();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(openAiResp, Map.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Map.of("ingredients", ingredients, "cuisine", cuisine, "dietaryRestrictions", dietaryRestrictions);
        }
    }

}
