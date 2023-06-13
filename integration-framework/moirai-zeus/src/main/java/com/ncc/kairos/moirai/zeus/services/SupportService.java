package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.FaqCategoryRepository;
import com.ncc.kairos.moirai.zeus.dao.FaqLinkRepository;
import com.ncc.kairos.moirai.zeus.dao.FaqRepository;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupportService {

    @Autowired
    FaqCategoryRepository faqCategoryRepository;
    @Autowired
    FaqRepository faqRepository;
    @Autowired
    FaqLinkRepository faqLinkRepository;

    public Faq retrieveFaq(String id) {
        Faq faq = faqRepository.existsById(id) ?
                faqRepository.findById(id).get() : null;
        return addLinksToFaq(faq);
    }

    public List<FaqCategory> retrieveFaqCategories() {
        List<FaqCategory> categories = faqCategoryRepository.findAll();
        for (FaqCategory category: categories) {
            category.setFaqs(addLinksToFaqList(category.getFaqs()));
        }

        return categories;
    }

    public void deleteFaq(String id) {
        try {
            faqLinkRepository.deleteByFaqId(id);
        } catch (EmptyResultDataAccessException e) {
            //Do nothing
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed deleting faq dependency.", e);
        }

        try {
            faqRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Can't delete object with ID that does not exist.", e);
        }
    }

    public void submitFaqCategories(List<FaqCategory> faqCategory) {
        faqCategory.forEach(category -> {
            if (!StringUtils.isEmpty(category.getId()) && faqCategoryRepository.existsById(category.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Category with ID " + category.getId() + " already created.");
            }
        });
        faqCategoryRepository.saveAll(faqCategory);
    }

    public void deleteFaqCategory(String id) {
        try {
            faqCategoryRepository.deleteById(id);
        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't delete object with ID that does not exist.");
        }
    }

    public FaqCategory retrieveFaqCategory(String id) {
        return faqCategoryRepository.existsById(id) ?
                faqCategoryRepository.findById(id).get() : null;
    }

    public void updateFaqById(String id, Faq faq) {
        if (!faqRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faq with Id " + id + " not found.");
        }
        faq.setId(id);
        saveFaqWithHtml(faq);
    }

    public void updateFaqCategoryById(String id, FaqCategory faqCategory) {
        if (!faqCategoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faq with Id " + id + " not found.");
        }
        faqCategory.setId(id);
        faqCategoryRepository.save(faqCategory);
    }

    @Transactional
    public void deleteFaqByIdList(List<String> idList) {
        faqLinkRepository.deleteByFaqIdIn(idList);
        faqRepository.deleteByIdIn(idList);
    }

    private Faq saveFaqWithHtml(Faq faq) {
        Matcher m = Constants.HREF_PATTERN.matcher((faq.getAnswer()));
        String safeAnswer = faq.getAnswer();

        int linkIndex = 0;
        List<FaqLink> links = new ArrayList<>();
        while (m.find()) {
            String html = m.group();
            String linkName = "HTML_LINK_" + linkIndex++;

            FaqLink newLink = new FaqLink();
            newLink.setHref(html.split("\"")[1]);
            newLink.setText(html.split(">")[1].split("<")[0]);
            newLink.setName(linkName);
            links.add(newLink);

            //We will swap the html in the faq for the new linkName
            safeAnswer = safeAnswer.replace(html, linkName);
        }

        faq.setAnswer(safeAnswer);
        faq = faqRepository.save(faq);

        // In case we already have an existing faq we need to clean any old links.
        faqLinkRepository.deleteByFaqId(faq.getId());

        for (FaqLink newLink: links) {
            newLink.setFaqId(faq.getId());
            faqLinkRepository.save(newLink);
        }

        return faq;
    }

    private Faq addLinksToFaq(Faq faq) {
        if (faq == null) {
            return null;
        }
        List<FaqLink> links = faqLinkRepository.findAllByFaqId(faq.getId());
        for (FaqLink link : links) {
            String html = "<a href=\"" + link.getHref() + "\">" + link.getText() + "</a>";
            String htmlAnswer = faq.getAnswer().replace(link.getName(), html);
            faq.setAnswer(htmlAnswer);
        }
        return faq;
    }

    private List<Faq> addLinksToFaqList(List<Faq> faqList) {
        return faqList.stream().map(faq -> addLinksToFaq(faq)).collect(Collectors.toList());
    }
}
