package by.bsuir.spp.autoservice.command.impl;

import by.bsuir.spp.autoservice.command.BaseCommand;
import by.bsuir.spp.autoservice.command.CommandException;
import by.bsuir.spp.autoservice.command.PagePath;
import by.bsuir.spp.autoservice.entity.DetailApplication;
import by.bsuir.spp.autoservice.service.DetailApplicationService;
import by.bsuir.spp.autoservice.service.ServiceException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class ShowFillDetailInvoiceCommand implements BaseCommand {
    private static final String REQUEST_ATTRIBUTE_NAME_APPLICATION_LIST = "detail_orders";

    @Override
    public PagePath execute(HttpServletRequest request) throws CommandException {
        PagePath page = null;
        try {
            DetailApplicationService service = DetailApplicationService.getInstance();
            List<DetailApplication> applications = service.findAllFreeApplications();
            request.setAttribute(REQUEST_ATTRIBUTE_NAME_APPLICATION_LIST, applications);
            page = PagePath.DETAIL_ORDERS;
        } catch (ServiceException ex) {
            throw new CommandException(ex);
        }

        return page;
    }
}
