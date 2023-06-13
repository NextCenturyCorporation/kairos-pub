package com.ncc.kairos.moirai.zeus.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ncc.kairos.moirai.zeus.dao.FaqCategoryRepository;
import com.ncc.kairos.moirai.zeus.dao.FaqLinkRepository;
import com.ncc.kairos.moirai.zeus.dao.FaqRepository;
import com.ncc.kairos.moirai.zeus.model.Faq;
import com.ncc.kairos.moirai.zeus.model.FaqCategory;
import com.ncc.kairos.moirai.zeus.model.FaqLink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@TestPropertySource(locations = "classpath:test.properties")
public class SupportServiceTest {

    @Autowired
    SupportService supportService;

    @Spy
    FaqCategoryRepository faqCategoryRepository;

    @MockBean
    FaqRepository faqRepository;

    @MockBean
    FaqLinkRepository faqLinkRepository;

    private Faq faq;

    private List<Faq> faqs = new ArrayList<>();

    private FaqCategory faqCategory;

    void updateReflections() {
        ReflectionTestUtils.setField(supportService, "faqCategoryRepository", faqCategoryRepository);
        ReflectionTestUtils.setField(supportService, "faqRepository", faqRepository);
        ReflectionTestUtils.setField(supportService, "faqLinkRepository", faqLinkRepository);
    }

    @BeforeEach
    void setup() {
        faqCategory = new FaqCategory().categoryName("cata").id(UUID.randomUUID().toString());
        faq = new Faq()
        .question("huh?")
        .id(UUID.randomUUID().toString())
        .answer("<a href=\"somethingsomething\">This is the link and answer</a>");
        faqs.add(faq);

        updateReflections();
    }

    @Test
    void addLinksToFaqTest() {
        Mockito.when(faqRepository.existsById(anyString())).thenReturn(true);
        Mockito.when(faqRepository.findById(anyString())).thenReturn(Optional.of(faq));
        List<FaqLink> links = new ArrayList<>();
        links.add(new FaqLink().faqId(UUID.randomUUID().toString()).href("somethingbetter").name("name1").text("text"));
        Mockito.when(faqLinkRepository.findAllByFaqId(faq.getId())).thenReturn(links);

        Faq result = supportService.retrieveFaq(faq.getId());
        assert result != null;

        Mockito.when(faqRepository.existsById(anyString())).thenReturn(false);
        result = supportService.retrieveFaq(faq.getId());
        assert result == null;
    }

    @Test
    void retrieveFaqCategoriesTest() {
        List<FaqCategory> returnThis = new ArrayList<>();
        Mockito.when(faqCategoryRepository.findAll()).thenReturn(returnThis);
        List<FaqCategory> result = supportService.retrieveFaqCategories();
        assert result.isEmpty();
    }

    @Test
    void deleteFaqTest() {
        Mockito.doNothing().when(faqLinkRepository).deleteByFaqId(anyString());
        Mockito.doNothing().when(faqRepository).deleteById(anyString());

        supportService.deleteFaq(faq.getId());

        verify(faqLinkRepository, times(1)).deleteByFaqId(anyString());
        verify(faqRepository, times(1)).deleteById(anyString());
    }
    
    @Test
    void submitFaqCategoriesTest() {
        Mockito.when(faqCategoryRepository.saveAll(anyIterable())).thenReturn(anyIterable());
        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(false);

        List<FaqCategory> list = new ArrayList<>();
        list.add(faqCategory);
        supportService.submitFaqCategories(list);
        verify(faqCategoryRepository, times(1)).saveAll(any());

        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> {
            supportService.submitFaqCategories(list);
        });
    }

    @Test
    void deleteFaqCategoryTest() {
        Mockito.doNothing().when(faqCategoryRepository).deleteById(faqCategory.getId());
        supportService.deleteFaqCategory(faqCategory.getId());
        verify(faqCategoryRepository, times(1)).deleteById(any());

        Mockito.doThrow(EmptyResultDataAccessException.class).when(faqCategoryRepository).deleteById(faqCategory.getId());
        assertThrows(ResponseStatusException.class, () -> {
            supportService.deleteFaqCategory(faqCategory.getId());
        });
    }

    @Test
    void retrieveFaqCategoryTest() {
        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(false);
        FaqCategory result = supportService.retrieveFaqCategory(faqCategory.getId());
        assert result == null;

        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(true);
        Mockito.when(faqCategoryRepository.findById(faqCategory.getId())).thenReturn(Optional.of(faqCategory));
        result = supportService.retrieveFaqCategory(faqCategory.getId());
        assert result.equals(faqCategory);
    }

    @Test
    void updateFaqByIdTest() {
        Mockito.when(faqRepository.existsById(faq.getId())).thenReturn(false);

        // Does not exist
        assertThrows(ResponseStatusException.class, () -> {
            supportService.updateFaqById(faq.getId(), faq);
        });

        Mockito.when(faqRepository.existsById(faq.getId())).thenReturn(true);
        Mockito.when(faqRepository.save(faq)).thenReturn(faq);
        Mockito.doNothing().when(faqLinkRepository).deleteByFaqId(anyString());

        //Happy path
        supportService.updateFaqById(faq.getId(), faq);
        verify(faqLinkRepository, times(1)).deleteByFaqId(any());
    }

    @Test
    void updateFaqCategoryByIdTest() {
        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(false);
        // Does not exist
        assertThrows(ResponseStatusException.class, () -> {
            supportService.updateFaqCategoryById(faqCategory.getId(), faqCategory);
        });

        // Happy path
        Mockito.when(faqCategoryRepository.existsById(faqCategory.getId())).thenReturn(true);
        Mockito.when(faqCategoryRepository.save(faqCategory)).thenReturn(faqCategory);
        supportService.updateFaqCategoryById(faqCategory.getId(), faqCategory);
        verify(faqCategoryRepository, times(1)).save(any());
    }

    @Test
    void deleteFaqByIdListTest() {
        List<String> idList = new ArrayList<>();
        idList.add(faq.getId());
        Mockito.doNothing().when(faqLinkRepository).deleteByFaqIdIn(idList);
        Mockito.doNothing().when(faqRepository).deleteByIdIn(idList);
        supportService.deleteFaqByIdList(idList);
        verify(faqLinkRepository, times(1)).deleteByFaqIdIn(anyList());
        verify(faqRepository, times(1)).deleteByIdIn(anyList());
    }

}
